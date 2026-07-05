package com.cipherlink.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LiveLocationResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userAvatarUrl;
    private Double latitude;
    private Double longitude;
    private String label;
    private LocalDateTime sharedAt;
    private LocalDateTime expiresAt;
    private boolean isActive;
}

