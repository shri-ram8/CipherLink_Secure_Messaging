package com.cipherlink.service;

import com.cipherlink.dto.*;
import com.cipherlink.model.*;
import com.cipherlink.repository.*;
import com.cipherlink.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepo;
    private final GroupMemberRepository groupMemberRepo;
    private final GroupMessageRepository groupMessageRepo;
    private final GroupMessageSeenRepository groupMessageSeenRepo;
    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final DeviceRepository deviceRepo;

    @Transactional
    public GroupResponse createGroup(UUID creatorUserId, CreateGroupRequest request) {
        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdByUserId(creatorUserId)
                .build();
        groupRepo.save(group);

        groupMemberRepo.save(GroupMember.builder()
                .groupId(group.getId()).userId(creatorUserId).role(GroupRole.OWNER).build());

        for (UUID userId : request.getMemberUserIds()) {
            if (userId.equals(creatorUserId)) continue;
            if (!userRepo.existsById(userId)) continue;
            groupMemberRepo.save(GroupMember.builder()
                    .groupId(group.getId()).userId(userId).role(GroupRole.MEMBER).build());
        }

        return buildGroupResponse(group.getId(), creatorUserId);
    }

    @Transactional
    public GroupResponse updateGroup(UUID userId, UUID groupId, UpdateGroupRequest request) {
        GroupMember member = groupMemberRepo.findByGroupIdAndUserIdAndIsRemovedFalse(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Not a group member"));
        if (member.getRole() == GroupRole.MEMBER)
            throw new RuntimeException("Only admins can update group");

        Group group = groupRepo.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setGroupPictureUrl(request.getGroupPictureUrl());
        groupRepo.save(group);

        return buildGroupResponse(groupId, userId);
    }

    @Transactional
    public void addMembers(UUID userId, UUID groupId, AddMembersRequest request) {
        GroupMember member = groupMemberRepo.findByGroupIdAndUserIdAndIsRemovedFalse(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Not a group member"));
        if (member.getRole() == GroupRole.MEMBER)
            throw new RuntimeException("Only admins can add members");

        for (UUID newUserId : request.getUserIds()) {
            if (groupMemberRepo.existsByGroupIdAndUserIdAndIsRemovedFalse(groupId, newUserId)) continue;
            groupMemberRepo.save(GroupMember.builder()
                    .groupId(groupId).userId(newUserId).role(GroupRole.MEMBER).build());
        }
    }

    @Transactional
    public void removeMember(UUID userId, UUID groupId, UUID targetUserId) {
        GroupMember requester = groupMemberRepo.findByGroupIdAndUserIdAndIsRemovedFalse(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Not a group member"));
        if (requester.getRole() == GroupRole.MEMBER)
            throw new RuntimeException("Only admins can remove members");

        GroupMember target = groupMemberRepo.findByGroupIdAndUserIdAndIsRemovedFalse(groupId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not in group"));
        if (target.getRole() == GroupRole.OWNER)
            throw new RuntimeException("Cannot remove group owner");

        target.setRemoved(true);
        groupMemberRepo.save(target);
    }

    @Transactional
    public void leaveGroup(UUID userId, UUID groupId) {
        GroupMember member = groupMemberRepo.findByGroupIdAndUserIdAndIsRemovedFalse(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Not a group member"));
        if (member.getRole() == GroupRole.OWNER)
            throw new RuntimeException("Owner cannot leave — transfer ownership first");

        member.setRemoved(true);
        groupMemberRepo.save(member);
    }

    @Transactional
    public void promoteToAdmin(UUID userId, UUID groupId, UUID targetUserId) {
        GroupMember requester = groupMemberRepo.findByGroupIdAndUserIdAndIsRemovedFalse(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Not a group member"));
        if (requester.getRole() != GroupRole.OWNER)
            throw new RuntimeException("Only owner can promote members");

        GroupMember target = groupMemberRepo.findByGroupIdAndUserIdAndIsRemovedFalse(groupId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not in group"));

        target.setRole(GroupRole.ADMIN);
        groupMemberRepo.save(target);
    }

    public List<GroupResponse> getMyGroups(UUID userId) {
        return groupMemberRepo.findGroupIdsByUserId(userId).stream()
                .map(groupId -> buildGroupResponse(groupId, userId))
                .collect(Collectors.toList());
    }

    public List<GroupMemberResponse> getMembers(UUID userId, UUID groupId) {
        if (!groupMemberRepo.existsByGroupIdAndUserIdAndIsRemovedFalse(groupId, userId))
            throw new RuntimeException("Not a group member");

        return groupMemberRepo.findByGroupIdAndIsRemovedFalse(groupId).stream().map(m -> {
            UserProfile profile = profileRepo.findByUserId(m.getUserId()).orElse(null);
            return GroupMemberResponse.builder()
                    .userId(m.getUserId())
                    .phoneNumber(m.getUser().getPhoneNumber())
                    .displayName(profile != null ? profile.getDisplayName() : "")
                    .role(m.getRole())
                    .joinedAt(m.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    public List<GroupMessageResponse> getGroupMessages(UUID userId, UUID groupId, int page, int pageSize) {
        if (!groupMemberRepo.existsByGroupIdAndUserIdAndIsRemovedFalse(groupId, userId))
            throw new RuntimeException("Not a group member");

        List<GroupMessage> messages = groupMessageRepo
                .findByGroupIdAndIsDeletedFalseOrderBySentAtDesc(groupId);

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, messages.size());
        if (fromIndex >= messages.size()) return Collections.emptyList();

        return messages.subList(fromIndex, toIndex).stream().map(msg -> {
            Device senderDevice = deviceRepo.findById(msg.getSenderDeviceId()).orElse(null);
            UserProfile senderProfile = senderDevice != null
                    ? profileRepo.findByUserId(senderDevice.getUserId()).orElse(null) : null;

            return GroupMessageResponse.builder()
                    .id(msg.getId())
                    .groupId(msg.getGroupId())
                    .senderDeviceId(msg.getSenderDeviceId())
                    .senderName(senderProfile != null ? senderProfile.getDisplayName() : "Unknown")
                    .encryptedPayload(msg.getEncryptedPayload())
                    .sentAt(msg.getSentAt())
                    .isDeleted(msg.isDeleted())
                    .seenByCount(msg.getSeenBy().size())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void markGroupMessageSeen(UUID userId, UUID groupMessageId) {
        if (groupMessageSeenRepo.existsByGroupMessageIdAndUserId(groupMessageId, userId)) return;

        groupMessageSeenRepo.save(GroupMessageSeen.builder()
                .groupMessageId(groupMessageId).userId(userId).build());
    }

    public GroupResponse buildGroupResponse(UUID groupId, UUID userId) {
        Group group = groupRepo.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
        long memberCount = groupMemberRepo.countByGroupIdAndIsRemovedFalse(groupId);
        GroupMember myMember = groupMemberRepo.findByGroupIdAndUserIdAndIsRemovedFalse(groupId, userId).orElse(null);

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .groupPictureUrl(group.getGroupPictureUrl())
                .createdByUserId(group.getCreatedByUserId())
                .createdAt(group.getCreatedAt())
                .memberCount((int) memberCount)
                .myRole(myMember != null ? myMember.getRole() : GroupRole.MEMBER)
                .build();
    }
}
