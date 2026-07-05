package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userProfilePic;
    private String type;
    private String contentUrl;
    private String caption;
    private String backgroundColor;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private int viewCount;
    private boolean hasViewed;
}
