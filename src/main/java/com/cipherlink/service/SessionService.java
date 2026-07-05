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
public class SessionService {

    private final SessionRepository sessionRepo;

    @Transactional
    public SessionResponse initiateSession(UUID senderDeviceId, InitiateSessionRequest request) {
        Session existing = sessionRepo
                .findBySenderDeviceIdAndReceiverDeviceId(senderDeviceId, request.getReceiverDeviceId())
                .orElse(null);

        if (existing != null) {
            return SessionResponse.builder()
                    .sessionId(existing.getId())
                    .receiverDeviceId(existing.getReceiverDeviceId())
                    .isEstablished(existing.isEstablished())
                    .createdAt(existing.getCreatedAt())
                    .build();
        }

        Session session = Session.builder()
                .senderDeviceId(senderDeviceId)
                .receiverDeviceId(request.getReceiverDeviceId())
                .ephemeralPublicKey(request.getEphemeralPublicKey())
                .isEstablished(true)
                .build();
        sessionRepo.save(session);

        return SessionResponse.builder()
                .sessionId(session.getId())
                .receiverDeviceId(session.getReceiverDeviceId())
                .isEstablished(session.isEstablished())
                .createdAt(session.getCreatedAt())
                .build();
    }

    public boolean sessionExists(UUID senderDeviceId, UUID receiverDeviceId) {
        return sessionRepo.existsBySenderDeviceIdAndReceiverDeviceIdAndIsEstablishedTrue(
                senderDeviceId, receiverDeviceId);
    }
}
