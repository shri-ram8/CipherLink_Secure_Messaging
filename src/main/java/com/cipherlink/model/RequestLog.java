package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "request_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String ipAddress;
    private String endpoint;
    private String method;
    private int statusCode;

    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    private UUID userId;
}
