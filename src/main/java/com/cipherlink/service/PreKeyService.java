package com.cipherlink.service;

import com.cipherlink.dto.*;
import com.cipherlink.model.*;
import com.cipherlink.repository.*;
import com.cipherlink.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreKeyService {

    private final DeviceRepository deviceRepo;
    private final PreKeyRepository preKeyRepo;
    private final SignedPreKeyRepository signedPreKeyRepo;
    private static final int MIN_PREKEY_THRESHOLD = 10;

    @Transactional
    public void uploadPreKeys(UUID userId, UploadPreKeysRequest request) {
        Device device = deviceRepo.findByIdAndUserIdAndIsRevokedFalse(request.getDeviceId(), userId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        for (PreKeyDto dto : request.getPreKeys()) {
            if (preKeyRepo.existsByDeviceIdAndPreKeyId(device.getId(), dto.getPreKeyId())) continue;
            preKeyRepo.save(PreKey.builder()
                    .deviceId(device.getId())
                    .preKeyId(dto.getPreKeyId())
                    .publicKey(dto.getPublicKey())
                    .build());
        }

        signedPreKeyRepo.findFirstByDeviceId(device.getId())
                .ifPresent(signedPreKeyRepo::delete);

        signedPreKeyRepo.save(SignedPreKey.builder()
                .deviceId(device.getId())
                .signedPreKeyId(request.getSignedPreKey().getSignedPreKeyId())
                .publicKey(request.getSignedPreKey().getPublicKey())
                .signature(request.getSignedPreKey().getSignature())
                .build());

        device.setLastSeenAt(LocalDateTime.now());
        deviceRepo.save(device);
    }

    @Transactional
    public PreKeyBundleResponse getPreKeyBundle(UUID targetUserId, UUID targetDeviceId) {
        Device device = deviceRepo.findByIdAndUserIdAndIsRevokedFalse(targetDeviceId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        PreKey preKey = preKeyRepo.findFirstByDeviceIdAndIsUsedFalse(targetDeviceId)
                .orElseThrow(() -> new RuntimeException("No PreKeys available"));

        preKey.setUsed(true);
        preKey.setUsedAt(LocalDateTime.now());
        preKeyRepo.save(preKey);

        SignedPreKey signedPreKey = signedPreKeyRepo.findFirstByDeviceId(targetDeviceId)
                .orElseThrow(() -> new RuntimeException("No SignedPreKey found"));

        return PreKeyBundleResponse.builder()
                .deviceId(device.getId())
                .identityPublicKey(device.getIdentityPublicKey())
                .preKey(new PreKeyDto(preKey.getPreKeyId(), preKey.getPublicKey()))
                .signedPreKey(new SignedPreKeyDto(
                        signedPreKey.getSignedPreKeyId(),
                        signedPreKey.getPublicKey(),
                        signedPreKey.getSignature()))
                .build();
    }

    public PreKeyCountResponse getPreKeyCount(UUID userId, UUID deviceId) {
        deviceRepo.findByIdAndUserIdAndIsRevokedFalse(deviceId, userId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        long count = preKeyRepo.countByDeviceIdAndIsUsedFalse(deviceId);
        return PreKeyCountResponse.builder()
                .deviceId(deviceId)
                .remainingPreKeys((int) count)
                .needsRefill(count < MIN_PREKEY_THRESHOLD)
                .build();
    }
}
