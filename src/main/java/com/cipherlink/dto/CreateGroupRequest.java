package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequest {
    private String name;
    private String description;
    private List<UUID> memberUserIds = new ArrayList<>();
}
