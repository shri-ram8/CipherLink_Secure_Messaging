package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "group_messages", indexes = {
    @Index(name = "idx_group_messages_group_id", columnList = "groupId"),
    @Index(name = "idx_group_messages_sent_at", columnList = "sentAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID groupId;

    @Column(nullable = false)
    private UUID senderDeviceId;

    @Column(columnDefinition = "TEXT")
    private String encryptedPayload;

    // Reply
    private UUID replyToMessageId;

    @Column(columnDefinition = "TEXT")
    private String replyPreview;

    // Edit
    @Column(columnDefinition = "TEXT")
    private String editedPayload;

    private LocalDateTime editedAt;

    @Builder.Default
    private boolean isEdited = false;

    // Pin
    @Builder.Default
    private boolean isPinned = false;

    // Media
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    // Voice
    private Integer voiceDurationSeconds;

    // Link preview
    @Column(columnDefinition = "TEXT")
    private String linkPreviewJson;

    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    @Builder.Default
    private boolean isDeleted = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String reactionsJson = "{}";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupId", insertable = false, updatable = false)
    private Group group;

    @OneToMany(mappedBy = "groupMessage", cascade = CascadeType.ALL)
    @Builder.Default
    private List<GroupMessageSeen> seenBy = new ArrayList<>();
}
