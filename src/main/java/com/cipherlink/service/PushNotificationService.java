package com.cipherlink.service;

import com.cipherlink.model.DeviceToken;
import com.cipherlink.model.UserProfile;
import com.cipherlink.repository.DeviceTokenRepository;
import com.cipherlink.repository.UserProfileRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepo;
    private final UserProfileRepository profileRepo;

    @Transactional
    public void registerToken(UUID userId, com.cipherlink.dto.RegisterFcmTokenRequest request) {
        DeviceToken existing = deviceTokenRepo
                .findByUserIdAndDeviceId(userId, request.getDeviceId()).orElse(null);

        if (existing != null) {
            existing.setFcmToken(request.getFcmToken());
            existing.setUpdatedAt(LocalDateTime.now());
            deviceTokenRepo.save(existing);
        } else {
            deviceTokenRepo.save(com.cipherlink.model.DeviceToken.builder()
                    .userId(userId)
                    .deviceId(request.getDeviceId())
                    .fcmToken(request.getFcmToken())
                    .build());
        }
    }

    /**
     * Send a new message push notification to the receiver.
     */
    public void sendMessageNotification(UUID senderUserId, UUID receiverUserId,
                                         String conversationId, String messagePreview) {
        List<DeviceToken> tokens = deviceTokenRepo.findByUserId(receiverUserId);
        if (tokens.isEmpty()) return;

        UserProfile senderProfile = profileRepo.findByUserId(senderUserId).orElse(null);
        String senderName = senderProfile != null ? senderProfile.getDisplayName() : "CipherLink";

        for (DeviceToken dt : tokens) {
            try {
                Message fcmMsg = Message.builder()
                        .setToken(dt.getFcmToken())
                        .setNotification(Notification.builder()
                                .setTitle(senderName)
                                .setBody(messagePreview != null ? messagePreview : "New message")
                                .build())
                        .putData("type", "NEW_MESSAGE")
                        .putData("conversationId", conversationId)
                        .putData("senderName", senderName)
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build())
                        .setApnsConfig(ApnsConfig.builder()
                                .setAps(Aps.builder()
                                        .setSound("default")
                                        .setBadge(1)
                                        .build())
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(fcmMsg);
            } catch (FirebaseMessagingException e) {
                log.warn("FCM send failed for token {}: {}", dt.getFcmToken(), e.getMessage());
                // Optionally remove invalid tokens
                if ("registration-token-not-registered".equals(e.getErrorCode())) {
                    deviceTokenRepo.delete(dt);
                }
            }
        }
    }

    /**
     * Send an incoming call notification.
     */
    public void sendCallNotification(UUID callerUserId, UUID receiverUserId,
                                      String callType, String roomId) {
        List<DeviceToken> tokens = deviceTokenRepo.findByUserId(receiverUserId);
        if (tokens.isEmpty()) return;

        UserProfile callerProfile = profileRepo.findByUserId(callerUserId).orElse(null);
        String callerName = callerProfile != null ? callerProfile.getDisplayName() : "Unknown";

        for (DeviceToken dt : tokens) {
            try {
                Message fcmMsg = Message.builder()
                        .setToken(dt.getFcmToken())
                        .setNotification(Notification.builder()
                                .setTitle("Incoming " + callType.toLowerCase() + " call")
                                .setBody(callerName + " is calling you")
                                .build())
                        .putData("type", "INCOMING_CALL")
                        .putData("callType", callType)
                        .putData("roomId", roomId)
                        .putData("callerName", callerName)
                        .putData("callerId", callerUserId.toString())
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(fcmMsg);
            } catch (FirebaseMessagingException e) {
                log.warn("FCM call notification failed: {}", e.getMessage());
            }
        }
    }
}
