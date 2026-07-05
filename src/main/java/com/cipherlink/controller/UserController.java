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
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PrivacyService privacyService;
    private final MediaService mediaService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal CipherLinkUserDetails user) {
        try {
            return ResponseEntity.ok(userService.getProfile(user.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody UpdateProfileRequest request) {
        try {
            return ResponseEntity.ok(userService.updateProfile(user.getUserId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/profile/avatar")
    public ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody UploadAvatarRequest request) {
        try {
            String avatarUrl = mediaService.uploadAvatar(user.getUserId(), request);
            userService.updateProfilePicture(user.getUserId(), avatarUrl);
            return ResponseEntity.ok(Map.of("profilePictureUrl", avatarUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/contacts")
    public ResponseEntity<?> addContact(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody AddContactRequest request) {
        try {
            return ResponseEntity.ok(userService.addContact(user.getUserId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/contacts")
    public ResponseEntity<?> getContacts(@AuthenticationPrincipal CipherLinkUserDetails user) {
        try {
            return ResponseEntity.ok(userService.getContacts(user.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/contacts/{contactUserId}/block")
    public ResponseEntity<?> blockContact(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @PathVariable UUID contactUserId) {
        try {
            userService.blockContact(user.getUserId(), contactUserId);
            return ResponseEntity.ok(Map.of("message", "Contact blocked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── Privacy ──────────────────────────────────────────────

    @GetMapping("/privacy")
    public ResponseEntity<?> getPrivacySettings(@AuthenticationPrincipal CipherLinkUserDetails user) {
        try {
            return ResponseEntity.ok(privacyService.getPrivacySettings(user.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/privacy")
    public ResponseEntity<?> updatePrivacySettings(
            @AuthenticationPrincipal CipherLinkUserDetails user,
            @RequestBody UpdatePrivacySettingsRequest request) {
        try {
            return ResponseEntity.ok(privacyService.updatePrivacySettings(user.getUserId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
