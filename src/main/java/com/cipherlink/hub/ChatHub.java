package com.cipherlink.hub;

import com.cipherlink.dto.*;
import com.cipherlink.model.*;
import com.cipherlink.repository.*;
import com.cipherlink.security.JwtService;
import com.cipherlink.service.PushNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatHub {

    private final SimpMessagingTemplate messaging;
    private final MessageRepository messageRepo;
    private final ConversationRepository conversationRepo;
    private final GroupMemberRepository groupMemberRepo;
    private final GroupMessageRepository groupMessageRepo;
    private final UserProfileRepository profileRepo;
    private final DeviceRepository deviceRepo;
    private final UserRepository userRepo;
    private final UserPrivacySettingsRepository privacyRepo;
    private final JwtService jwtService;
    private final PushNotificationService pushService;
    private final ObjectMapper objectMapper;

    // userId → sessionId
    private static final Map<UUID, String> connectedUsers = new ConcurrentHashMap<>();

    // ── Connect / Disconnect ──────────────────────────────

    @Transactional
    @MessageMapping("/connect")
    public void onConnect(@Header("Authorization") String authHeader,
                          StompHeaderAccessor accessor) {
        UUID userId = extractUserIdFromHeader(authHeader);
        if (userId == null) return;

        connectedUsers.put(userId, accessor.getSessionId());
        profileRepo.updateOnlineStatus(userId, true, LocalDateTime.now());

        // Notify contacts that this user is online
        messaging.convertAndSend("/topic/presence",
                Map.of("event", "UserOnline", "userId", userId));
    }

    @Transactional
    @MessageMapping("/disconnect")
    public void onDisconnect(@Header("Authorization") String authHeader) {
        UUID userId = extractUserIdFromHeader(authHeader);
        if (userId == null) return;

        connectedUsers.remove(userId);
        profileRepo.updateOnlineStatus(userId, false, LocalDateTime.now());

        messaging.convertAndSend("/topic/presence",
                Map.of("event", "UserOffline", "userId", userId));
    }

    // ── 1-to-1 messages ───────────────────────────────────

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload SendMessageRequest request,
                            @Header("Authorization") String authHeader) {
        UUID senderUserId = extractUserIdFromHeader(authHeader);
        log.info("[WS-DEBUG] chat.sendMessage received. senderUserId={} conversationId={}", senderUserId, request.getConversationId());
        if (senderUserId == null) return;

        Device device = deviceRepo.findFirstByUserIdAndIsRevokedFalseOrderByCreatedAtDesc(senderUserId)
                .orElse(null);
        if (device == null) return;

        Conversation conv = conversationRepo.findById(request.getConversationId()).orElse(null);
        if (conv == null) return;

        // Build message
        Message message = Message.builder()
                .conversationId(request.getConversationId())
                .senderDeviceId(device.getId())
                .encryptedPayload(request.getEncryptedPayload())
                .replyToMessageId(request.getReplyToMessageId())
                .replyPreview(request.getReplyPreview())
                .mediaUrl(request.getMediaUrl())
                .voiceDurationSeconds(request.getVoiceDurationSeconds())
                .locationLat(request.getLocationLat())
                .locationLng(request.getLocationLng())
                .locationLabel(request.getLocationLabel())
                .status(MessageStatus.SENT)
                .build();

        if (request.getMediaType() != null) {
            try { message.setMediaType(MediaType.valueOf(request.getMediaType().toUpperCase())); }
            catch (Exception ignored) {}
        }

        messageRepo.save(message);

        // Update last message timestamp
        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepo.save(conv);

        UUID receiverUserId = conv.getUser1Id().equals(senderUserId)
                ? conv.getUser2Id() : conv.getUser1Id();

        UserProfile senderProfile = profileRepo.findByUserId(senderUserId).orElse(null);
        String senderName = senderProfile != null ? senderProfile.getDisplayName() : "Unknown";

        Map<String, Object> envelope = Map.of(
                "event", "ReceiveMessage",
                "data", buildMessagePayload(message, false, senderName));

        // Deliver to receiver if online
        log.info("[WS-DEBUG] receiverUserId={} isInConnectedUsersMap={}", receiverUserId, connectedUsers.containsKey(receiverUserId));
        if (connectedUsers.containsKey(receiverUserId)) {
            messaging.convertAndSendToUser(
                    receiverUserId.toString(), "/queue/messages", envelope);
            log.info("[WS-DEBUG] convertAndSendToUser called for receiver={}", receiverUserId);
            message.setStatus(MessageStatus.DELIVERED);
            messageRepo.save(message);
        } else {
            // Send FCM push notification
            String preview = buildPreviewText(message);
            pushService.sendMessageNotification(senderUserId, receiverUserId,
                    request.getConversationId().toString(), preview);
        }

        // Confirm to sender — include clientTempId (if the client sent one)
        // so the frontend can swap its optimistic bubble for the real one
        // instead of appending a duplicate.
        log.info("[WS-DEBUG] sending MessageSent confirmation to sender={}", senderUserId);
        Map<String, Object> senderEnvelope = new HashMap<>();
        senderEnvelope.put("event", "MessageSent");
        senderEnvelope.put("data", buildMessagePayload(message, true, senderName));
        if (request.getClientTempId() != null) {
            senderEnvelope.put("clientTempId", request.getClientTempId());
        }
        messaging.convertAndSendToUser(
                senderUserId.toString(), "/queue/messages", senderEnvelope);
    }

    // ── Message seen ──────────────────────────────────────

    @MessageMapping("/chat.markSeen")
    public void markSeen(@Payload MarkSeenRequest request,
                         @Header("Authorization") String authHeader) {
        UUID userId = extractUserIdFromHeader(authHeader);
        if (userId == null) return;

        messageRepo.findById(request.getMessageId()).ifPresent(message -> {
            message.setStatus(MessageStatus.SEEN);
            message.setSeenAt(LocalDateTime.now());
            messageRepo.save(message);

            UUID senderUserId = getOwnerUserId(message.getSenderDeviceId());
            if (connectedUsers.containsKey(senderUserId) && canShowReadReceipts(senderUserId, userId)) {
                messaging.convertAndSendToUser(
                        senderUserId.toString(), "/queue/messages",
                        Map.of("event", "MessageSeen",
                                "messageId", message.getId(),
                                "seenAt", message.getSeenAt()));
            }
        });
    }

    // viewerUserId wants to know if targetUserId has read their message;
    // targetUserId's own privacy setting controls whether that's allowed.
    private boolean canShowReadReceipts(UUID viewerUserId, UUID targetUserId) {
        UserPrivacySettings privacy = privacyRepo.findByUserId(targetUserId).orElse(null);
        if (privacy == null) return true;
        return switch (privacy.getReadReceiptsVisibility()) {
            case "NOBODY" -> false;
            default -> true; // EVERYONE / CONTACTS — contact-scoping not enforced here, matches MessageService's current behavior
        };
    }

    // ── React to message ──────────────────────────────────

    @MessageMapping("/chat.react")
    public void reactToMessage(@Payload Map<String, String> payload,
                               @Header("Authorization") String authHeader) {
        UUID userId = extractUserIdFromHeader(authHeader);
        if (userId == null) return;

        UUID messageId = UUID.fromString(payload.get("messageId"));
        String emoji = payload.get("emoji"); // null or empty = remove

        messageRepo.findById(messageId).ifPresent(message -> {
            try {
                Map<String, List<String>> reactions = objectMapper.readValue(
                        message.getReactionsJson() != null ? message.getReactionsJson() : "{}",
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, List<String>>>() {});

                String userIdStr = userId.toString();
                reactions.forEach((k, v) -> v.remove(userIdStr));
                reactions.entrySet().removeIf(e -> e.getValue().isEmpty());

                if (emoji != null && !emoji.isBlank()) {
                    reactions.computeIfAbsent(emoji, k -> new ArrayList<>()).add(userIdStr);
                }

                message.setReactionsJson(objectMapper.writeValueAsString(reactions));
                messageRepo.save(message);

                // Broadcast reaction update to both participants
                Conversation conv = conversationRepo.findById(message.getConversationId()).orElse(null);
                if (conv != null) {
                    Map<String, Object> reactionEvent = Map.of(
                            "event", "MessageReactionUpdated",
                            "messageId", messageId,
                            "userId", userId,
                            "emoji", emoji != null ? emoji : "",
                            "reactions", reactions.entrySet().stream()
                                    .collect(java.util.stream.Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> e.getValue().size())));

                    for (UUID uid : List.of(conv.getUser1Id(), conv.getUser2Id())) {
                        if (connectedUsers.containsKey(uid)) {
                            messaging.convertAndSendToUser(uid.toString(), "/queue/messages", reactionEvent);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Reaction update failed: {}", e.getMessage());
            }
        });
    }

    // ── Edit message ──────────────────────────────────────

    @MessageMapping("/chat.editMessage")
    public void editMessage(@Payload Map<String, String> payload,
                            @Header("Authorization") String authHeader) {
        UUID userId = extractUserIdFromHeader(authHeader);
        if (userId == null) return;

        UUID messageId = UUID.fromString(payload.get("messageId"));
        String newPayload = payload.get("newPayload");

        messageRepo.findById(messageId).ifPresent(message -> {
            // Verify ownership
            UUID ownerUserId = getOwnerUserId(message.getSenderDeviceId());
            if (!ownerUserId.equals(userId)) return;

            message.setEditedPayload(newPayload);
            message.setEdited(true);
            message.setEditedAt(LocalDateTime.now());
            messageRepo.save(message);

            // Broadcast edit to conversation
            Conversation conv = conversationRepo.findById(message.getConversationId()).orElse(null);
            if (conv != null) {
                Map<String, Object> editEvent = Map.of(
                        "event", "MessageEdited",
                        "messageId", messageId,
                        "newPayload", newPayload,
                        "editedAt", message.getEditedAt());

                for (UUID uid : List.of(conv.getUser1Id(), conv.getUser2Id())) {
                    if (connectedUsers.containsKey(uid)) {
                        messaging.convertAndSendToUser(uid.toString(), "/queue/messages", editEvent);
                    }
                }
            }
        });
    }

    // ── Delete message broadcast ──────────────────────────

    @MessageMapping("/chat.deleteMessage")
    public void deleteMessage(@Payload Map<String, String> payload,
                              @Header("Authorization") String authHeader) {
        UUID userId = extractUserIdFromHeader(authHeader);
        if (userId == null) return;

        UUID messageId = UUID.fromString(payload.get("messageId"));

        messageRepo.findById(messageId).ifPresent(message -> {
            UUID ownerUserId = getOwnerUserId(message.getSenderDeviceId());
            if (!ownerUserId.equals(userId)) return;

            UUID conversationId = message.getConversationId();

            message.setDeleted(true);
            message.setEncryptedPayload(null);
            message.setEditedPayload(null);
            message.setLinkPreviewJson(null);
            messageRepo.save(message);

            // Tell the other side to remove the message immediately
            Conversation conv = conversationRepo.findById(conversationId).orElse(null);
            if (conv != null) {
                Map<String, Object> deleteEvent = Map.of(
                        "event", "MessageDeleted",
                        "messageId", messageId,
                        "conversationId", conversationId);

                for (UUID uid : List.of(conv.getUser1Id(), conv.getUser2Id())) {
                    if (connectedUsers.containsKey(uid)) {
                        messaging.convertAndSendToUser(uid.toString(), "/queue/messages", deleteEvent);
                    }
                }
            }
        });
    }

    // ── Typing ────────────────────────────────────────────

    @MessageMapping("/chat.typingStarted")
    public void typingStarted(@Payload Map<String, String> payload,
                              @Header("Authorization") String authHeader) {
        UUID userId = extractUserIdFromHeader(authHeader);
        if (userId == null) return;

        UUID conversationId = UUID.fromString(payload.get("conversationId"));
        conversationRepo.findById(conversationId).ifPresent(conv -> {
            UUID other = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
            if (connectedUsers.containsKey(other)) {
                messaging.convertAndSendToUser(other.toString(), "/queue/typing",
                        Map.of("event", "UserTyping", "userId", userId, "conversationId", conversationId));
            }
        });
    }

    @MessageMapping("/chat.typingStopped")
    public void typingStopped(@Payload Map<String, String> payload,
                              @Header("Authorization") String authHeader) {
        UUID userId = extractUserIdFromHeader(authHeader);
        if (userId == null) return;

        UUID conversationId = UUID.fromString(payload.get("conversationId"));
        conversationRepo.findById(conversationId).ifPresent(conv -> {
            UUID other = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
            if (connectedUsers.containsKey(other)) {
                messaging.convertAndSendToUser(other.toString(), "/queue/typing",
                        Map.of("event", "UserStoppedTyping", "userId", userId, "conversationId", conversationId));
            }
        });
    }

    // ── Group messages ────────────────────────────────────

    @MessageMapping("/chat.sendGroupMessage")
    public void sendGroupMessage(@Payload SendGroupMessageRequest request,
                                 @Header("Authorization") String authHeader) {
        UUID senderUserId = extractUserIdFromHeader(authHeader);
        if (senderUserId == null) return;

        boolean isMember = groupMemberRepo
                .existsByGroupIdAndUserIdAndIsRemovedFalse(request.getGroupId(), senderUserId);
        if (!isMember) return;

        Device device = deviceRepo.findFirstByUserIdAndIsRevokedFalseOrderByCreatedAtDesc(senderUserId)
                .orElse(null);
        if (device == null) return;

        GroupMessage message = GroupMessage.builder()
                .groupId(request.getGroupId())
                .senderDeviceId(device.getId())
                .encryptedPayload(request.getEncryptedPayload())
                .build();
        groupMessageRepo.save(message);

        UserProfile senderProfile = profileRepo.findByUserId(senderUserId).orElse(null);
        String senderName = senderProfile != null ? senderProfile.getDisplayName() : "Unknown";

        Map<String, Object> response = Map.of(
                "event", "ReceiveGroupMessage",
                "data", Map.of(
                        "id", message.getId(),
                        "groupId", message.getGroupId(),
                        "senderUserId", senderUserId,
                        "senderName", senderName,
                        "encryptedPayload", message.getEncryptedPayload(),
                        "sentAt", message.getSentAt()));

        List<UUID> memberIds = groupMemberRepo.findMemberIds(request.getGroupId());
        for (UUID memberId : memberIds) {
            if (!memberId.equals(senderUserId) && connectedUsers.containsKey(memberId)) {
                messaging.convertAndSendToUser(memberId.toString(), "/queue/group-messages", response);
            }
        }
    }

    // ── WebRTC call signaling ─────────────────────────────

    @MessageMapping("/call.initiate")
    public void initiateCall(@Payload Map<String, String> payload,
                             @Header("Authorization") String authHeader) {
        UUID callerId = extractUserIdFromHeader(authHeader);
        if (callerId == null) return;

        UUID receiverId = UUID.fromString(payload.get("receiverId"));
        String callType = payload.get("callType");
        String roomId = payload.get("roomId");
        String offer = payload.get("offer"); // WebRTC SDP

        UserProfile callerProfile = profileRepo.findByUserId(callerId).orElse(null);
        String callerName = callerProfile != null ? callerProfile.getDisplayName() : "Unknown";

        Map<String, Object> callEvent = new HashMap<>();
        callEvent.put("event", "IncomingCall");
        callEvent.put("callerId", callerId);
        callEvent.put("callerName", callerName);
        callEvent.put("callerAvatarUrl", callerProfile != null ? callerProfile.getProfilePictureUrl() : null);
        callEvent.put("callType", callType);
        callEvent.put("roomId", roomId);
        callEvent.put("offer", offer);

        if (connectedUsers.containsKey(receiverId)) {
            messaging.convertAndSendToUser(receiverId.toString(), "/queue/calls", callEvent);
        } else {
            // Send FCM push for incoming call
            pushService.sendCallNotification(callerId, receiverId, callType, roomId);
        }
    }

    @MessageMapping("/call.answer")
    public void answerCall(@Payload Map<String, String> payload,
                           @Header("Authorization") String authHeader) {
        UUID answeredByUserId = extractUserIdFromHeader(authHeader);
        if (answeredByUserId == null) return;

        UUID callerId = UUID.fromString(payload.get("callerId"));
        String answer = payload.get("answer"); // WebRTC SDP answer
        String roomId = payload.get("roomId");

        if (connectedUsers.containsKey(callerId)) {
            messaging.convertAndSendToUser(callerId.toString(), "/queue/calls",
                    Map.of("event", "CallAnswered",
                            "answeredBy", answeredByUserId,
                            "answer", answer,
                            "roomId", roomId));
        }
    }

    @MessageMapping("/call.end")
    public void endCall(@Payload Map<String, String> payload,
                        @Header("Authorization") String authHeader) {
        UUID userId = extractUserIdFromHeader(authHeader);
        if (userId == null) return;

        UUID otherUserId = UUID.fromString(payload.get("otherUserId"));
        String reason = payload.getOrDefault("reason", "ENDED");

        if (connectedUsers.containsKey(otherUserId)) {
            messaging.convertAndSendToUser(otherUserId.toString(), "/queue/calls",
                    Map.of("event", "CallEnded", "reason", reason, "by", userId));
        }
    }

    @MessageMapping("/call.ice")
    public void iceCandidate(@Payload Map<String, String> payload,
                             @Header("Authorization") String authHeader) {
        UUID userId = extractUserIdFromHeader(authHeader);
        if (userId == null) return;

        UUID targetUserId = UUID.fromString(payload.get("targetUserId"));
        String candidate = payload.get("candidate");

        if (connectedUsers.containsKey(targetUserId)) {
            messaging.convertAndSendToUser(targetUserId.toString(), "/queue/calls",
                    Map.of("event", "IceCandidate", "candidate", candidate, "from", userId));
        }
    }

    // ── Live location broadcast ───────────────────────────

    @MessageMapping("/location.update")
    public void broadcastLocation(@Payload Map<String, Object> payload,
                                  @Header("Authorization") String authHeader) {
        UUID userId = extractUserIdFromHeader(authHeader);
        if (userId == null) return;

        String conversationIdStr = (String) payload.get("conversationId");
        if (conversationIdStr == null) return;

        UUID conversationId = UUID.fromString(conversationIdStr);
        Conversation conv = conversationRepo.findById(conversationId).orElse(null);
        if (conv == null) return;

        UserProfile profile = profileRepo.findByUserId(userId).orElse(null);

        Map<String, Object> locationEvent = new HashMap<>(payload);
        locationEvent.put("event", "LocationUpdate");
        locationEvent.put("userId", userId);
        locationEvent.put("userName", profile != null ? profile.getDisplayName() : "");

        UUID other = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
        if (connectedUsers.containsKey(other)) {
            messaging.convertAndSendToUser(other.toString(), "/queue/location", locationEvent);
        }
    }

    // ── Helpers ───────────────────────────────────────────

    private UUID extractUserIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        try {
            if (!jwtService.isTokenValid(token)) return null;
            return jwtService.extractUserId(token);
        } catch (Exception e) {
            return null;
        }
    }

    private UUID getOwnerUserId(UUID deviceId) {
        return deviceRepo.findById(deviceId)
                .map(Device::getUserId)
                .orElse(null);
    }

    private Map<String, Object> buildMessagePayload(Message m, boolean isMine, String senderName) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", m.getId());
        data.put("conversationId", m.getConversationId());
        data.put("senderDeviceId", m.getSenderDeviceId());
        data.put("encryptedPayload", m.getEncryptedPayload());
        data.put("sentAt", m.getSentAt());
        data.put("status", m.getStatus().name());
        data.put("isMine", isMine);
        data.put("isDeleted", false);
        data.put("senderName", senderName);
        if (m.getReplyToMessageId() != null) {
            data.put("replyToMessageId", m.getReplyToMessageId());
            data.put("replyPreview", m.getReplyPreview());
        }
        if (m.getMediaUrl() != null) {
            data.put("mediaUrl", m.getMediaUrl());
            data.put("mediaType", m.getMediaType() != null ? m.getMediaType().name() : null);
            data.put("voiceDurationSeconds", m.getVoiceDurationSeconds());
        }
        if (m.getLocationLat() != null) {
            data.put("locationLat", m.getLocationLat());
            data.put("locationLng", m.getLocationLng());
            data.put("locationLabel", m.getLocationLabel());
        }
        return data;
    }

    private String buildPreviewText(Message m) {
        if (m.getMediaType() != null) {
            return switch (m.getMediaType()) {
                case IMAGE -> "📷 Photo";
                case VIDEO -> "🎥 Video";
                case AUDIO -> "🎤 Voice message";
                case DOCUMENT -> "📄 Document";
                default -> "Media";
            };
        }
        if (m.getLocationLat() != null) return "📍 Location";
        String payload = m.getEncryptedPayload();
        if (payload == null) return "Message";
        return payload.length() > 50 ? payload.substring(0, 50) + "..." : payload;
    }

    public static boolean isOnline(UUID userId) {
        return connectedUsers.containsKey(userId);
    }
}
