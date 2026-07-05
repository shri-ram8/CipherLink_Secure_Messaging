package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_conversation_id", columnList = "conversationId"),
    @Index(name = "idx_messages_sent_at", columnList = "sentAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID conversationId;

    @Column(nullable = false)
    private UUID senderDeviceId;

    @Column(columnDefinition = "TEXT")
    private String encryptedPayload;

    // Reply support
    private UUID replyToMessageId;

    @Column(columnDefinition = "TEXT")
    private String replyPreview;

    // Edit support
    @Column(columnDefinition = "TEXT")
    private String editedPayload;

    private LocalDateTime editedAt;

    @Builder.Default
    private boolean isEdited = false;

    // Pin support
    @Builder.Default
    private boolean isPinned = false;

    private LocalDateTime pinnedAt;
    private UUID pinnedByUserId;

    // Media
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    // Location
    private Double locationLat;
    private Double locationLng;
    private String locationLabel;

    // Voice note
    private Integer voiceDurationSeconds;

    // Link preview
    @Column(columnDefinition = "TEXT")
    private String linkPreviewJson;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    private LocalDateTime seenAt;

    @Builder.Default
    private boolean isDeleted = false;

    // Reactions stored as JSON: {"👍": ["userId1","userId2"], "❤️": ["userId3"]}
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String reactionsJson = "{}";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversationId", insertable = false, updatable = false)
    private Conversation conversation;
}
