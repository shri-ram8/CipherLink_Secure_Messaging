package com.cipherlink.controller;

import com.cipherlink.dto.*;
import com.cipherlink.model.CallRecord;
import com.cipherlink.security.CipherLinkUserDetails;
import com.cipherlink.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

// ── Calls ──────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/calls")
@RequiredArgsConstructor
class CallController {

    private final CallService callService;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiateCall(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody InitiateCallRequest request) {
        try {
            CallRecord call = callService.initiateCall(user.getUserId(), request);
            return ResponseEntity.ok(Map.of(
                    "callId", call.getId(),
                    "roomId", call.getRoomId(),
                    "status", call.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{callId}/end")
    public ResponseEntity<?> endCall(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID callId,
            @RequestBody EndCallRequest request) {
        try {
            callService.endCall(user.getUserId(), callId, request.getStatus(), request.getDurationSeconds());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getCallHistory(@AuthenticationPrincipal CipherLinkUserDetails user) {
        try {
            return ResponseEntity.ok(callService.getCallHistory(user.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/missed/count")
    public ResponseEntity<?> getMissedCallCount(@AuthenticationPrincipal CipherLinkUserDetails user) {
        try {
            return ResponseEntity.ok(Map.of("count", callService.getMissedCallCount(user.getUserId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

// ── Live Location ──────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
class LiveLocationController {

    private final LiveLocationService liveLocationService;

    @PostMapping("/start")
    public ResponseEntity<?> startSharing(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody StartLiveLocationRequest request) {
        try {
            return ResponseEntity.ok(liveLocationService.startSharing(user.getUserId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{locationId}")
    public ResponseEntity<?> updateLocation(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID locationId,
            @RequestBody UpdateLiveLocationRequest request) {
        try {
            return ResponseEntity.ok(liveLocationService.updateLocation(user.getUserId(), locationId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<?> stopSharing(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID locationId) {
        try {
            liveLocationService.stopSharing(user.getUserId(), locationId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getActiveLocations(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID conversationId) {
        try {
            return ResponseEntity.ok(liveLocationService.getActiveLocations(conversationId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
