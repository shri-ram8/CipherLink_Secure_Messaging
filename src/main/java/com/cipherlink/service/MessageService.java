package com.cipherlink.service;

import com.cipherlink.dto.*;
import com.cipherlink.model.*;
import com.cipherlink.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final DeviceRepository deviceRepo;
    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final UserPrivacySettingsRepository privacyRepo;
    private final LinkPreviewService linkPreviewService;
    private final PushNotificationService pushService;
    private final ObjectMapper objectMapper;

    // ── Create / Get conversation ──────────────────────────

    @Transactional
    public ConversationResponse createConversation(UUID userId, CreateConversationRequest request) {
        User receiver = userRepo.findByPhoneNumber(com.cipherlink.helper.PhoneNumberUtil.normalize(request.getReceiverPhoneNumber()))
                .orElseThrow(() -> new RuntimeException("User not found on CipherLink"));

        Conversation existing = conversationRepo.findByUsers(userId, receiver.getId()).orElse(null);
        if (existing != null) return buildConversationResponse(existing, userId);

        Conversation conversation = Conversation.builder()
                .user1Id(userId)
                .user2Id(receiver.getId())
                .build();
        conversationRepo.save(conversation);
        return buildConversationResponse(conversation, userId);
    }

    public List<ConversationResponse> getConversations(UUID userId) {
        return conversationRepo.findByUserId(userId).stream()
                .map(c -> buildConversationResponse(c, userId))
                .collect(Collectors.toList());
    }

    // ── Get messages (paged) ───────────────────────────────

    public PagedResult<MessageResponse> getMessages(UUID userId, UUID conversationId, int page, int pageSize) {
        // Verify user is in this conversation
        Conversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        List<UUID> myDeviceIds = deviceRepo.findByUserId(userId).stream()
                .map(Device::getId).collect(Collectors.toList());

        long total = messageRepo.countByConversationIdAndIsDeletedFalse(conversationId);
        List<Message> messages = messageRepo.findPagedMessages(
                conversationId, PageRequest.of(page - 1, pageSize));

        // Check receiver privacy for read receipts
        UUID otherUserId = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
        boolean showReadReceipts = canShowReadReceipts(userId, otherUserId);

        List<MessageResponse> result = messages.stream()
                .map(m -> buildMessageResponse(m, myDeviceIds, showReadReceipts))
                .collect(Collectors.toList());

        return PagedResult.<MessageResponse>builder()
                .items(result)
                .totalCount((int) total)
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    // ── Delete (Instagram style – hard remove from response) ──

    @Transactional
    public void deleteMessage(UUID userId, UUID messageId) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        deviceRepo.findById(message.getSenderDeviceId())
                .filter(d -> d.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        // Hard delete: flag + clear payload completely
        message.setDeleted(true);
        message.setEncryptedPayload(null);
        message.setEditedPayload(null);
        message.setReplyPreview(null);
        message.setMediaUrl(null);
        message.setLinkPreviewJson(null);
        messageRepo.save(message);
    }

    // ── Edit message ───────────────────────────────────────

    @Transactional
    public MessageResponse editMessage(UUID userId, UUID messageId, String newPayload) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        deviceRepo.findById(message.getSenderDeviceId())
                .filter(d -> d.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        if (message.isDeleted()) throw new RuntimeException("Cannot edit deleted message");

        message.setEditedPayload(newPayload);
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        messageRepo.save(message);

        List<UUID> myDeviceIds = deviceRepo.findByUserId(userId).stream()
                .map(Device::getId).collect(Collectors.toList());
        return buildMessageResponse(message, myDeviceIds, true);
    }

    // ── React to message ───────────────────────────────────

    @Transactional
    public MessageResponse reactToMessage(UUID userId, UUID messageId, String emoji) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        if (message.isDeleted()) throw new RuntimeException("Cannot react to deleted message");

        try {
            Map<String, List<String>> reactions = objectMapper.readValue(
                    message.getReactionsJson(),
                    new TypeReference<Map<String, List<String>>>() {});

            String userIdStr = userId.toString();

            // Remove user from all existing reactions first
            reactions.forEach((key, list) -> list.remove(userIdStr));
            reactions.entrySet().removeIf(e -> e.getValue().isEmpty());

            // Add new reaction if provided
            if (emoji != null && !emoji.isBlank()) {
                reactions.computeIfAbsent(emoji, k -> new ArrayList<>()).add(userIdStr);
            }

            message.setReactionsJson(objectMapper.writeValueAsString(reactions));
            messageRepo.save(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to update reaction");
        }

        List<UUID> myDeviceIds = deviceRepo.findByUserId(userId).stream()
                .map(Device::getId).collect(Collectors.toList());
        return buildMessageResponse(message, myDeviceIds, true);
    }

    // ── Pin / Unpin message ────────────────────────────────

    @Transactional
    public MessageResponse pinMessage(UUID userId, UUID messageId, boolean pin) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Verify user is in this conversation
        Conversation conv = conversationRepo.findById(message.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        message.setPinned(pin);
        message.setPinnedAt(pin ? LocalDateTime.now() : null);
        message.setPinnedByUserId(pin ? userId : null);
        messageRepo.save(message);

        List<UUID> myDeviceIds = deviceRepo.findByUserId(userId).stream()
                .map(Device::getId).collect(Collectors.toList());
        return buildMessageResponse(message, myDeviceIds, true);
    }

    // ── Get pinned messages ────────────────────────────────

    public List<MessageResponse> getPinnedMessages(UUID userId, UUID conversationId) {
        Conversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        List<UUID> myDeviceIds = deviceRepo.findByUserId(userId).stream()
                .map(Device::getId).collect(Collectors.toList());

        return messageRepo
                .findByConversationIdAndIsPinnedTrueAndIsDeletedFalseOrderByPinnedAtDesc(conversationId)
                .stream()
                .map(m -> buildMessageResponse(m, myDeviceIds, true))
                .collect(Collectors.toList());
    }

    // ── Search messages ────────────────────────────────────

    public List<MessageResponse> searchMessages(UUID userId, UUID conversationId, String query) {
        Conversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        List<UUID> myDeviceIds = deviceRepo.findByUserId(userId).stream()
                .map(Device::getId).collect(Collectors.toList());

        return messageRepo.searchMessages(conversationId, query, PageRequest.of(0, 50))
                .stream()
                .map(m -> buildMessageResponse(m, myDeviceIds, true))
                .collect(Collectors.toList());
    }

    // ── Forward message ────────────────────────────────────

    @Transactional
    public void forwardMessage(UUID userId, UUID messageId,
                               List<UUID> targetConvIds, List<UUID> targetGroupIds) {
        Message src = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        if (src.isDeleted()) throw new RuntimeException("Cannot forward deleted message");

        UUID myDeviceId = deviceRepo.findFirstByUserIdAndIsRevokedFalseOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new RuntimeException("No active device"))
                .getId();

        if (targetConvIds != null) {
            for (UUID convId : targetConvIds) {
                Conversation conv = conversationRepo.findById(convId).orElse(null);
                if (conv == null) continue;
                if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) continue;

                Message fwd = Message.builder()
                        .conversationId(convId)
                        .senderDeviceId(myDeviceId)
                        .encryptedPayload(src.getEncryptedPayload())
                        .mediaUrl(src.getMediaUrl())
                        .mediaType(src.getMediaType())
                        .voiceDurationSeconds(src.getVoiceDurationSeconds())
                        .linkPreviewJson(src.getLinkPreviewJson())
                        .build();
                messageRepo.save(fwd);
                updateConversationLastMessage(convId);
            }
        }
    }

    // ── Link preview ───────────────────────────────────────

    public LinkPreviewDto getLinkPreview(String url) {
        return linkPreviewService.fetchPreview(url);
    }

    // ── Mark conversation as seen ──────────────────────────

    @Transactional
    public void markConversationSeen(UUID userId, UUID conversationId) {
        List<UUID> myDeviceIds = deviceRepo.findByUserId(userId).stream()
                .map(Device::getId).collect(Collectors.toList());
        messageRepo.markConversationAsSeen(conversationId, myDeviceIds);
    }

    // ── Helpers ────────────────────────────────────────────

    private MessageResponse buildMessageResponse(Message m, List<UUID> myDeviceIds, boolean showReadReceipts) {
        boolean isMine = myDeviceIds.contains(m.getSenderDeviceId());

        // Parse reactions
        Map<String, Long> reactionCounts = new LinkedHashMap<>();
        String myReaction = null;
        try {
            Map<String, List<String>> raw = objectMapper.readValue(
                    m.getReactionsJson() != null ? m.getReactionsJson() : "{}",
                    new TypeReference<Map<String, List<String>>>() {});
            for (Map.Entry<String, List<String>> e : raw.entrySet()) {
                reactionCounts.put(e.getKey(), (long) e.getValue().size());
                if (isMine && e.getValue().stream().anyMatch(id ->
                        myDeviceIds.stream().map(UUID::toString).anyMatch(id::equals))) {
                    myReaction = e.getKey();
                }
            }
        } catch (Exception ignored) {}

        // Parse link preview
        LinkPreviewDto linkPreview = null;
        if (m.getLinkPreviewJson() != null) {
            try {
                linkPreview = objectMapper.readValue(m.getLinkPreviewJson(), LinkPreviewDto.class);
            } catch (Exception ignored) {}
        }

        // Respect read receipts privacy
        String status = m.getStatus() != null ? m.getStatus().name() : "SENT";
        if (!showReadReceipts && "SEEN".equals(status) && !isMine) {
            status = "DELIVERED";
        }

        // Active payload = editedPayload if edited, else encryptedPayload
        String payload = m.isEdited() && m.getEditedPayload() != null
                ? m.getEditedPayload()
                : m.getEncryptedPayload();

        return MessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderDeviceId(m.getSenderDeviceId())
                .encryptedPayload(payload)
                .isMine(isMine)
                .status(status)
                .sentAt(m.getSentAt())
                .seenAt(m.getSeenAt())
                .isDeleted(m.isDeleted())
                .isEdited(m.isEdited())
                .editedAt(m.getEditedAt())
                .isPinned(m.isPinned())
                .replyToMessageId(m.getReplyToMessageId())
                .replyPreview(m.getReplyPreview())
                .mediaUrl(m.getMediaUrl())
                .mediaType(m.getMediaType() != null ? m.getMediaType().name() : null)
                .voiceDurationSeconds(m.getVoiceDurationSeconds())
                .locationLat(m.getLocationLat())
                .locationLng(m.getLocationLng())
                .locationLabel(m.getLocationLabel())
                .reactionCounts(reactionCounts.isEmpty() ? null : reactionCounts)
                .myReaction(myReaction)
                .linkPreview(linkPreview)
                .build();
    }

    public ConversationResponse buildConversationResponse(Conversation conversation, UUID userId) {
        UUID otherUserId = conversation.getUser1Id().equals(userId)
                ? conversation.getUser2Id() : conversation.getUser1Id();

        User otherUser = userRepo.findById(otherUserId).orElse(null);
        UserProfile otherProfile = profileRepo.findByUserId(otherUserId).orElse(null);

        Message lastMsg = messageRepo
                .findFirstByConversationIdAndIsDeletedFalseOrderBySentAtDesc(conversation.getId())
                .orElse(null);

        List<UUID> myDeviceIds = deviceRepo.findByUserId(userId).stream()
                .map(Device::getId).collect(Collectors.toList());
        long unreadCount = messageRepo.countUnreadMessages(conversation.getId(), myDeviceIds);

        // Respect online status privacy
        boolean showOnline = otherProfile != null && otherProfile.isOnline()
                && canShowOnlineStatus(userId, otherUserId);

        // Respect last seen privacy
        LocalDateTime lastSeen = canShowLastSeen(userId, otherUserId) && otherProfile != null
                ? otherProfile.getLastSeen() : null;

        String lastMsgPreview = null;
        if (lastMsg != null && !lastMsg.isDeleted()) {
            if (lastMsg.getMediaType() != null) {
                lastMsgPreview = switch (lastMsg.getMediaType()) {
                    case IMAGE -> "📷 Photo";
                    case VIDEO -> "🎥 Video";
                    case AUDIO -> "🎤 Voice message";
                    case DOCUMENT -> "📄 Document";
                    default -> "Media";
                };
            } else if (lastMsg.getLocationLat() != null) {
                lastMsgPreview = "📍 Location";
            } else {
                // For non-E2E preview: truncate plain text
                String payload = lastMsg.isEdited() ? lastMsg.getEditedPayload() : lastMsg.getEncryptedPayload();
                lastMsgPreview = payload != null && payload.length() > 60
                        ? payload.substring(0, 60) + "..." : payload;
            }
        }

        return ConversationResponse.builder()
                .id(conversation.getId())
                .otherUserId(otherUserId)
                .otherUserPhone(otherUser != null ? otherUser.getPhoneNumber() : "")
                .otherUserName(otherProfile != null ? otherProfile.getDisplayName() : "")
                .otherUserAvatarUrl(otherProfile != null ? otherProfile.getProfilePictureUrl() : null)
                .lastMessage(lastMsgPreview)
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount((int) unreadCount)
                .isOnline(showOnline)
                .lastSeen(lastSeen)
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private void updateConversationLastMessage(UUID conversationId) {
        conversationRepo.findById(conversationId).ifPresent(conv -> {
            conv.setLastMessageAt(LocalDateTime.now());
            conversationRepo.save(conv);
        });
    }

    private boolean canShowOnlineStatus(UUID viewerUserId, UUID targetUserId) {
        UserPrivacySettings privacy = privacyRepo.findByUserId(targetUserId).orElse(null);
        if (privacy == null) return true;
        return switch (privacy.getOnlineStatusVisibility()) {
            case "EVERYONE" -> true;
            case "NOBODY" -> false;
            case "CONTACTS" -> isContact(viewerUserId, targetUserId);
            default -> true;
        };
    }

    private boolean canShowLastSeen(UUID viewerUserId, UUID targetUserId) {
        UserPrivacySettings privacy = privacyRepo.findByUserId(targetUserId).orElse(null);
        if (privacy == null) return true;
        return switch (privacy.getLastSeenVisibility()) {
            case "EVERYONE" -> true;
            case "NOBODY" -> false;
            case "CONTACTS" -> isContact(viewerUserId, targetUserId);
            default -> true;
        };
    }

    private boolean canShowReadReceipts(UUID viewerUserId, UUID targetUserId) {
        UserPrivacySettings privacy = privacyRepo.findByUserId(targetUserId).orElse(null);
        if (privacy == null) return true;
        return switch (privacy.getReadReceiptsVisibility()) {
            case "EVERYONE" -> true;
            case "NOBODY" -> false;
            case "CONTACTS" -> isContact(viewerUserId, targetUserId);
            default -> true;
        };
    }

    private boolean isContact(UUID ownerId, UUID contactId) {
        // Simple check — you can inject ContactRepository here if needed
        return true; // default: assume contact
    }

    private LocalDateTime java_time_LocalDateTime_getNow() {
        return LocalDateTime.now();
    }
}
