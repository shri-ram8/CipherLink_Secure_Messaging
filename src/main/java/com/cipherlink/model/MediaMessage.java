package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "media_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID messageId;

    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    private String fileUrl;
    private String storagePath;
    private String fileName;
    private long fileSizeBytes;
    private String thumbnailUrl;
    private String encryptedKey;
    private String nonce;

    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "messageId", insertable = false, updatable = false)
    private Message message;
}
