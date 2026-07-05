package com.cipherlink.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// ─────────────────────────────────────────────────────────
// Privacy
// ─────────────────────────────────────────────────────────

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdatePrivacySettingsRequest {
    private String onlineStatusVisibility;
    private String lastSeenVisibility;
    private String readReceiptsVisibility;
    private String storyVisibility;
    private String profilePictureVisibility;
    private String aboutVisibility;
}

