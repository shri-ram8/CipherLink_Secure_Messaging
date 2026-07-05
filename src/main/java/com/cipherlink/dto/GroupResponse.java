package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponse {
    private UUID id;
    private String name;
    private String description;
    private String groupPictureUrl;
    private UUID createdByUserId;
    private LocalDateTime createdAt;
    private int memberCount;
    private GroupRole myRole;
}
