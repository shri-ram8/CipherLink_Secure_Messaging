package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactResponse {
    private UUID contactUserId;
    private String phoneNumber;
    private String displayName;
    private String nickname;
    private boolean isBlocked;
    private boolean isOnCipherLink;
}
