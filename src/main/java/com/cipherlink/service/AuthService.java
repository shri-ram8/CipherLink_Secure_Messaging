package com.cipherlink.service;

import com.cipherlink.dto.*;
import com.cipherlink.model.*;
import com.cipherlink.repository.*;
import com.cipherlink.security.JwtService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final DeviceRepository deviceRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiry-days}")
    private int refreshTokenExpiryDays;

    // sendOtp is no longer needed — Firebase handles it on the frontend
    // This endpoint can return a simple acknowledgment
    public void sendOtp(String phoneNumber) {
        // Firebase sends OTP directly to user from frontend
        // Nothing to do here — kept for API compatibility
    }

    @Transactional
    public AuthResponse verifyOtpAndLogin(VerifyOtpRequest request) {
        // Verify Firebase ID token
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance()
                    .verifyIdToken(request.getFirebaseIdToken());

            String phoneNumber = decoded.getClaims()
                    .get("phone_number").toString();

            // Find or create user
            boolean isNewUser = false;
            User user = userRepo.findByPhoneNumber(phoneNumber).orElse(null);

            if (user == null) {
                user = User.builder()
                        .phoneNumber(phoneNumber)
                        .build();
                userRepo.save(user);
                isNewUser = true;
            }

            // Register device
            // Replace your current code with this:
            // Step 3 - Register device (reuse existing if available)
            final User finalUser = user;
            Device device = deviceRepo
                    .findFirstByUserIdAndIsRevokedFalseOrderByCreatedAtDesc(finalUser.getId())
                    .orElseGet(() -> Device.builder()
                            .userId(finalUser.getId())
                            .deviceName(request.getDeviceName())
                            .identityPublicKey(request.getIdentityPublicKey())
                            .devicePublicKey(request.getDevicePublicKey())
                            .build());
            deviceRepo.save(device);;

            // Generate tokens
            String accessToken = jwtService.generateAccessToken(user.getId(), device.getId());
            String refreshToken = jwtService.generateRefreshToken();

            refreshTokenRepo.save(RefreshToken.builder()
                    .userId(user.getId())
                    .token(refreshToken)
                    .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                    .build());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .deviceId(device.getId())
                    .isNewUser(isNewUser)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Invalid Firebase token: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepo
                .findByTokenAndIsRevokedFalseAndExpiresAtAfter(refreshToken, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired refresh token"));

        token.setRevoked(true);
        refreshTokenRepo.save(token);

        Device device = deviceRepo
                .findFirstByUserIdAndIsRevokedFalseOrderByCreatedAtDesc(token.getUserId())
                .orElseThrow(() -> new RuntimeException("No active device found"));

        String newAccessToken = jwtService.generateAccessToken(token.getUserId(), device.getId());
        String newRefreshToken = jwtService.generateRefreshToken();

        refreshTokenRepo.save(RefreshToken.builder()
                .userId(token.getUserId())
                .token(newRefreshToken)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .build());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(token.getUserId())
                .deviceId(device.getId())
                .isNewUser(false)
                .build();
    }
}