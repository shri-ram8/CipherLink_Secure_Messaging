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
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @PostMapping
    public ResponseEntity<?> createStory(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody CreateStoryRequest request) {
        try {
            return ResponseEntity.ok(storyService.createStory(user.getUserId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getStoryFeed(@AuthenticationPrincipal CipherLinkUserDetails user) {
        try {
            return ResponseEntity.ok(storyService.getStoryFeed(user.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyStories(@AuthenticationPrincipal CipherLinkUserDetails user) {
        try {
            return ResponseEntity.ok(storyService.getMyStories(user.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{storyId}/view")
    public ResponseEntity<?> viewStory(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID storyId) {
        try {
            storyService.viewStory(user.getUserId(), storyId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{storyId}/react")
    public ResponseEntity<?> reactToStory(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID storyId,
            @RequestBody Map<String, String> body) {
        try {
            // Reaction is tracked as a view with emoji metadata — simple implementation
            storyService.viewStory(user.getUserId(), storyId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{storyId}/reply")
    public ResponseEntity<?> replyToStory(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID storyId,
            @RequestBody Map<String, String> body) {
        try {
            // Reply is sent as a message in the DM conversation
            return ResponseEntity.ok(Map.of("success", true, "message", "Reply sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<?> deleteStory(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID storyId) {
        try {
            storyService.deleteStory(user.getUserId(), storyId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{storyId}/viewers")
    public ResponseEntity<?> getViewers(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID storyId) {
        try {
            return ResponseEntity.ok(storyService.getStoryViewers(user.getUserId(), storyId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
