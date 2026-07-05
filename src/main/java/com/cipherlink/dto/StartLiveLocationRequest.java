package com.cipherlink.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// ─────────────────────────────────────────────────────────
// Location
// ─────────────────────────────────────────────────────────

@Data @NoArgsConstructor @AllArgsConstructor
public class StartLiveLocationRequest {
    private UUID conversationId;
    private UUID groupId;
    private Double latitude;
    private Double longitude;
    private String label;
    private Integer durationMinutes;  // 15 | 60 | 480
}

