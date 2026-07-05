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
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepo;
    private final StoryRepository storyRepo;
    private final RequestLogRepository requestLogRepo;

    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000) // every 6 hours
    @Transactional
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();

        // Remove expired / revoked refresh tokens
        List<RefreshToken> expiredTokens = refreshTokenRepo.findByExpiresAtBeforeOrIsRevokedTrue(now);
        refreshTokenRepo.deleteAll(expiredTokens);

        // Expire stories
        List<Story> expiredStories = storyRepo.findByExpiresAtBeforeAndIsActiveTrue(now);
        expiredStories.forEach(s -> s.setActive(false));
        storyRepo.saveAll(expiredStories);

        // Remove old request logs (older than 7 days)
        List<RequestLog> oldLogs = requestLogRepo.findByRequestedAtBefore(now.minusDays(7));
        requestLogRepo.deleteAll(oldLogs);
    }
}
