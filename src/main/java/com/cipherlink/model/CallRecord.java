package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "call_records", indexes = {
    @Index(name = "idx_call_records_caller", columnList = "callerId"),
    @Index(name = "idx_call_records_receiver", columnList = "receiverId"),
    @Index(name = "idx_call_records_started_at", columnList = "startedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID callerId;

    @Column(nullable = false)
    private UUID receiverId;

    // "AUDIO" | "VIDEO"
    @Column(nullable = false)
    private String callType;

    // "COMPLETED" | "MISSED" | "DECLINED" | "FAILED"
    @Column(nullable = false)
    @Builder.Default
    private String status = "MISSED";

    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime answeredAt;
    private LocalDateTime endedAt;

    // Duration in seconds
    private Integer durationSeconds;

    // WebRTC offer/answer for reconnect edge case
    @Column(columnDefinition = "TEXT")
    private String roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "callerId", insertable = false, updatable = false)
    private User caller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiverId", insertable = false, updatable = false)
    private User receiver;
}
