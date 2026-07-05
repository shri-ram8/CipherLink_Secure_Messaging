package com.cipherlink.controller;

import com.cipherlink.dto.*;
import com.cipherlink.security.CipherLinkUserDetails;
import com.cipherlink.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController { 

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<?> createGroup(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody CreateGroupRequest request) {
        try {
            return ResponseEntity.ok(groupService.createGroup(user.getUserId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID groupId,
            @RequestBody UpdateGroupRequest request) {
        try {
            return ResponseEntity.ok(groupService.updateGroup(user.getUserId(), groupId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyGroups(@AuthenticationPrincipal CipherLinkUserDetails user) {
        try {
            return ResponseEntity.ok(groupService.getMyGroups(user.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getMembers(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID groupId) {
        try {
            return ResponseEntity.ok(groupService.getMembers(user.getUserId(), groupId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMembers(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID groupId,
            @RequestBody AddMembersRequest request) {
        try {
            groupService.addMembers(user.getUserId(), groupId, request);
            return ResponseEntity.ok(Map.of("message", "Members added"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<?> removeMember(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID groupId,
            @PathVariable UUID targetUserId) {
        try {
            groupService.removeMember(user.getUserId(), groupId, targetUserId);
            return ResponseEntity.ok(Map.of("message", "Member removed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<?> leaveGroup(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID groupId) {
        try {
            groupService.leaveGroup(user.getUserId(), groupId);
            return ResponseEntity.ok(Map.of("message", "Left group"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/promote/{targetUserId}")
    public ResponseEntity<?> promoteToAdmin(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID groupId,
            @PathVariable UUID targetUserId) {
        try {
            groupService.promoteToAdmin(user.getUserId(), groupId, targetUserId);
            return ResponseEntity.ok(Map.of("message", "Member promoted to admin"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<?> getMessages(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "1") int page) {
        try {
            return ResponseEntity.ok(groupService.getGroupMessages(user.getUserId(), groupId, page, 50));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/messages/{groupMessageId}/seen")
    public ResponseEntity<?> markSeen(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID groupMessageId) {
        try {
            groupService.markGroupMessageSeen(user.getUserId(), groupMessageId);
            return ResponseEntity.ok(Map.of("message", "Marked as seen"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
