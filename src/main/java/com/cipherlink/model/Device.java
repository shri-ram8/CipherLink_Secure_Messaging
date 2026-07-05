package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private String deviceName;
    private String identityPublicKey;
    private String devicePublicKey;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private boolean isRevoked = false;

    private LocalDateTime lastSeenAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PreKey> preKeys = new ArrayList<>();

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
    @Builder.Default
    private List<SignedPreKey> signedPreKeys = new ArrayList<>();
}
