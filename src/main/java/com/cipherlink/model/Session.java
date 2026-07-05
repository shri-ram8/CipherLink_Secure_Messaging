package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID senderDeviceId;
    private UUID receiverDeviceId;
    private String ephemeralPublicKey;

    @Builder.Default
    private boolean isEstablished = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senderDeviceId", insertable = false, updatable = false)
    private Device senderDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiverDeviceId", insertable = false, updatable = false)
    private Device receiverDevice;
}
