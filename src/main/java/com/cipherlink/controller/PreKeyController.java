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
@RequestMapping("/api/prekey")
@RequiredArgsConstructor
public class PreKeyController { 

    private final PreKeyService preKeyService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPreKeys(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody UploadPreKeysRequest request) {
        try {
            preKeyService.uploadPreKeys(user.getUserId(), request);
            return ResponseEntity.ok(Map.of("message", "PreKeys uploaded successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/bundle/{targetUserId}/{targetDeviceId}")
    public ResponseEntity<?> getPreKeyBundle(
            @PathVariable UUID targetUserId,
            @PathVariable UUID targetDeviceId) {
        try {
            return ResponseEntity.ok(preKeyService.getPreKeyBundle(targetUserId, targetDeviceId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/count/{deviceId}")
    public ResponseEntity<?> getPreKeyCount(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID deviceId) {
        try {
            return ResponseEntity.ok(preKeyService.getPreKeyCount(user.getUserId(), deviceId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
