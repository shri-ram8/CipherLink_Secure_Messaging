package com.cipherlink.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// ─────────────────────────────────────────────────────────
// Calls
// ─────────────────────────────────────────────────────────

@Data @NoArgsConstructor @AllArgsConstructor
public class InitiateCallRequest {
    private UUID receiverId;
    private String callType;   // "AUDIO" | "VIDEO"
    private String roomId;
}

