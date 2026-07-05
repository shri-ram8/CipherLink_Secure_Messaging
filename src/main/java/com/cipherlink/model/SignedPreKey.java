package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "signed_pre_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignedPreKey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID deviceId;
    private int signedPreKeyId;
    private String publicKey;
    private String signature;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deviceId", insertable = false, updatable = false)
    private Device device;
}
