package com.cipherlink.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class SendMessageRequest {
    private UUID conversationId;
    private String encryptedPayload;
    // Client-generated id used to reconcile this send with the optimistic
    // message the sender's own UI already rendered before the server ack.
    private String clientTempId;
    // Optional reply
    private UUID replyToMessageId;
    private String replyPreview;
    // Optional media
    private String mediaUrl;
    private String mediaType;
    private Integer voiceDurationSeconds;
    // Optional location
    private Double locationLat;
    private Double locationLng;
    private String locationLabel;
}

