package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String aboutText;

    private String profilePictureUrl;

    // Supabase Storage path (for signed URL generation)
    private String profilePictureStoragePath;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Online / last seen tracking
    @Builder.Default
    private boolean isOnline = false;

    private LocalDateTime lastSeen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;
}
