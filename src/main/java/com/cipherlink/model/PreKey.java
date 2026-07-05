package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "pre_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreKey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID deviceId;
    private int preKeyId;
    private String publicKey;

    @Builder.Default
    private boolean isUsed = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deviceId", insertable = false, updatable = false)
    private Device device;
}
