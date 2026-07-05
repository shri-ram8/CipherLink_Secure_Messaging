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
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final PushNotificationService pushService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMedia(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody UploadMediaRequest request) {
        try {
            return ResponseEntity.ok(mediaService.uploadMedia(user.getDeviceId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<?> getMedia(@PathVariable UUID messageId) {
        try {
            return mediaService.getMedia(messageId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/register-fcm")
    public ResponseEntity<?> registerFcmToken(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody RegisterFcmTokenRequest request) {
        try {
            pushService.registerToken(user.getUserId(), request);
            return ResponseEntity.ok(Map.of("message", "FCM token registered"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
