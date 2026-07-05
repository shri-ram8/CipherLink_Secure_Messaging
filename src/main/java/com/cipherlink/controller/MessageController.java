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
@RequestMapping("/api/v1/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final LinkPreviewService linkPreviewService;

    // ── Conversations ──────────────────────────────────────

    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody CreateConversationRequest request) {
        try {
            return ResponseEntity.ok(messageService.createConversation(user.getUserId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@AuthenticationPrincipal CipherLinkUserDetails user) {
        try {
            return ResponseEntity.ok(messageService.getConversations(user.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── Messages ───────────────────────────────────────────

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> getMessages(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        try {
            return ResponseEntity.ok(
                    messageService.getMessages(user.getUserId(), conversationId, page, pageSize));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/conversations/{conversationId}/seen")
    public ResponseEntity<?> markConversationSeen(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID conversationId) {
        try {
            messageService.markConversationSeen(user.getUserId(), conversationId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── Pinned messages ────────────────────────────────────

    @GetMapping("/conversations/{conversationId}/pinned")
    public ResponseEntity<?> getPinnedMessages(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID conversationId) {
        try {
            return ResponseEntity.ok(messageService.getPinnedMessages(user.getUserId(), conversationId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/messages/{messageId}/pin")
    public ResponseEntity<?> pinMessage(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID messageId,
            @RequestParam boolean pin) {
        try {
            return ResponseEntity.ok(messageService.pinMessage(user.getUserId(), messageId, pin));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── Search ─────────────────────────────────────────────

    @GetMapping("/conversations/{conversationId}/search")
    public ResponseEntity<?> searchMessages(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID conversationId,
            @RequestParam String q) {
        try {
            return ResponseEntity.ok(messageService.searchMessages(user.getUserId(), conversationId, q));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── Delete ─────────────────────────────────────────────

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID messageId) {
        try {
            messageService.deleteMessage(user.getUserId(), messageId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── Edit ───────────────────────────────────────────────

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<?> editMessage(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID messageId,
            @RequestBody EditMessageRequest request) {
        try {
            return ResponseEntity.ok(messageService.editMessage(user.getUserId(), messageId, request.getNewPayload()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── React ──────────────────────────────────────────────

    @PostMapping("/messages/{messageId}/react")
    public ResponseEntity<?> reactToMessage(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID messageId,
            @RequestBody ReactToMessageRequest request) {
        try {
            return ResponseEntity.ok(messageService.reactToMessage(user.getUserId(), messageId, request.getEmoji()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── Forward ────────────────────────────────────────────

    @PostMapping("/messages/{messageId}/forward")
    public ResponseEntity<?> forwardMessage(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID messageId,
            @RequestBody ForwardMessageRequest request) {
        try {
            messageService.forwardMessage(user.getUserId(), messageId,
                    request.getTargetConversationIds(), request.getTargetGroupIds());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── Link preview ───────────────────────────────────────

    @PostMapping("/link-preview")
    public ResponseEntity<?> getLinkPreview(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody LinkPreviewRequest request) {
        try {
            LinkPreviewDto preview = linkPreviewService.fetchPreview(request.getUrl());
            if (preview == null) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
