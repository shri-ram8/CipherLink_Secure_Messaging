package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberResponse {
    private UUID userId;
    private String phoneNumber;
    private String displayName;
    private GroupRole role;
    private LocalDateTime joinedAt;
}
