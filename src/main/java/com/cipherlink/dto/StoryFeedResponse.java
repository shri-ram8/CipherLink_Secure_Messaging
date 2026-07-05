package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryFeedResponse {
    private UUID userId;
    private String userName;
    private String userProfilePic;
    private List<StoryResponse> stories;
    private boolean hasUnviewed;
}
