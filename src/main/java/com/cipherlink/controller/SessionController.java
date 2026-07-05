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
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController { 

    private final SessionService sessionService;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiateSession(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody InitiateSessionRequest request) {
        try {
            return ResponseEntity.ok(sessionService.initiateSession(user.getDeviceId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/exists/{receiverDeviceId}")
    public ResponseEntity<?> sessionExists(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID receiverDeviceId) {
        try {
            boolean exists = sessionService.sessionExists(user.getDeviceId(), receiverDeviceId);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
