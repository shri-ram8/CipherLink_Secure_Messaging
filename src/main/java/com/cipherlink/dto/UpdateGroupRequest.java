package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupRequest {
    private String name;
    private String description;
    private String groupPictureUrl;
}
