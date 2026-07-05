package com.cipherlink.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {
    private UUID id;
    private UUID conversationId;
    private UUID senderDeviceId;
    private String encryptedPayload;
    @com.fasterxml.jackson.annotation.JsonProperty("isMine")
    private boolean isMine;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime seenAt;
    @com.fasterxml.jackson.annotation.JsonProperty("isDeleted")
    private boolean isDeleted;
    @com.fasterxml.jackson.annotation.JsonProperty("isEdited")
    private boolean isEdited;
    private LocalDateTime editedAt;
    @com.fasterxml.jackson.annotation.JsonProperty("isPinned")
    private boolean isPinned;

    // Reply
    private UUID replyToMessageId;
    private String replyPreview;

    // Media
    private String mediaUrl;
    private String mediaType;
    private Integer voiceDurationSeconds;

    // Location
    private Double locationLat;
    private Double locationLng;
    private String locationLabel;

    // Reactions: {"❤️": 2, "👍": 1}  + myReaction
    private Map<String, Long> reactionCounts;
    private String myReaction;

    // Link preview
    private LinkPreviewDto linkPreview;

    // Sender info (for group chat)
    private String senderName;
    private String senderAvatarUrl;
}
