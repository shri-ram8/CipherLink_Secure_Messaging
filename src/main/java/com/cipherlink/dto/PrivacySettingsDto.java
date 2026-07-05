package com.cipherlink.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrivacySettingsDto {
    private String onlineStatusVisibility;
    private String lastSeenVisibility;
    private String readReceiptsVisibility;
    private String storyVisibility;
    private String profilePictureVisibility;
    private String aboutVisibility;
}

