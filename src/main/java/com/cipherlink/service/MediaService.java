package com.cipherlink.service;

import com.cipherlink.dto.MediaMessageResponse;
import com.cipherlink.dto.UploadAvatarRequest;
import com.cipherlink.dto.UploadMediaRequest;
import com.cipherlink.model.*;
import com.cipherlink.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MessageRepository messageRepo;
    private final MediaMessageRepository mediaMessageRepo;
    private final ConversationRepository conversationRepo;
    private final DeviceRepository deviceRepo;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String supabaseServiceKey;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public MediaMessageResponse uploadMedia(UUID senderDeviceId, UploadMediaRequest request) {
        byte[] fileBytes = Base64.getDecoder().decode(request.getBase64FileData());

        Device device = deviceRepo.findById(senderDeviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        String ext = getExtension(request.getFileName());
        String storagePath = device.getUserId() + "/"
                + request.getConversationId() + "/"
                + UUID.randomUUID() + ext;

        String publicUrl = uploadToSupabase(storagePath, fileBytes,
                request.getMimeType() != null ? request.getMimeType() : "application/octet-stream");

        Message message = Message.builder()
                .conversationId(request.getConversationId())
                .senderDeviceId(senderDeviceId)
                .encryptedPayload(null)
                .mediaUrl(publicUrl)
                .voiceDurationSeconds(request.getVoiceDurationSeconds())
                .status(MessageStatus.SENT)
                .build();

        try {
            message.setMediaType(com.cipherlink.model.MediaType.valueOf(request.getMediaType().toUpperCase()));
        } catch (Exception ignored) {}

        messageRepo.save(message);

        MediaMessage media = MediaMessage.builder()
                .messageId(message.getId())
                .mediaType(message.getMediaType())
                .fileUrl(publicUrl)
                .storagePath(storagePath)
                .fileName(request.getFileName())
                .fileSizeBytes(fileBytes.length)
                .encryptedKey(request.getEncryptedKey())
                .nonce(request.getNonce())
                .build();
        mediaMessageRepo.save(media);

        conversationRepo.findById(request.getConversationId()).ifPresent(conv -> {
            conv.setLastMessageAt(LocalDateTime.now());
            conversationRepo.save(conv);
        });

        return MediaMessageResponse.builder()
                .messageId(message.getId())
                .fileUrl(publicUrl)
                .storagePath(storagePath)
                .fileName(request.getFileName())
                .mediaType(request.getMediaType())
                .fileSizeBytes((long) fileBytes.length)
                .uploadedAt(media.getUploadedAt())
                .build();
    }

    public String uploadAvatar(UUID userId, UploadAvatarRequest request) {
        byte[] fileBytes = Base64.getDecoder().decode(request.getBase64ImageData());
        String ext = getExtension(request.getFileName());
        String storagePath = "avatars/" + userId + ext;
        return uploadToSupabase(storagePath, fileBytes,
                request.getMimeType() != null ? request.getMimeType() : "image/jpeg");
    }

    private String uploadToSupabase(String path, byte[] data, String mimeType) {
        String endpoint = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

        HttpHeaders headers = buildHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType(mimeType));
        headers.set("x-upsert", "true");

        ResponseEntity<Map> response = restTemplate.exchange(
                URI.create(endpoint),
                HttpMethod.POST,
                new HttpEntity<>(data, headers),
                Map.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
        }
        throw new RuntimeException("Supabase upload failed: " + response.getStatusCode());
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseServiceKey);
        headers.set("Authorization", "Bearer " + supabaseServiceKey);
        return headers;
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public Optional<MediaMessage> getMedia(UUID messageId) {
        return mediaMessageRepo.findByMessageId(messageId);
    }
}
