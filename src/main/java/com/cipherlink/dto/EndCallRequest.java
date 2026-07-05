package com.cipherlink.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class EndCallRequest {
    private UUID callId;
    private String status;  // "COMPLETED" | "DECLINED"
    private Integer durationSeconds;
}

