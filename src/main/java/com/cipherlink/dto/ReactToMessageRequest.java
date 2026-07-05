package com.cipherlink.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// ─────────────────────────────────────────────────────────
// Message extras
// ─────────────────────────────────────────────────────────

@Data @NoArgsConstructor @AllArgsConstructor
public class ReactToMessageRequest {
    private String emoji;  // null to remove
}

