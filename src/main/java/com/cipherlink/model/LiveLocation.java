package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private UUID conversationId;
    private UUID groupId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String label;

    @Builder.Default
    private LocalDateTime sharedAt = LocalDateTime.now();

    // When sharing expires (user sets duration: 15min, 1hr, 8hr)
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;
}
