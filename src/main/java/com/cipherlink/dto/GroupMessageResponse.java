package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMessageResponse {
    private UUID id;
    private UUID groupId;
    private UUID senderDeviceId;
    private String senderName;
    private String encryptedPayload;
    private LocalDateTime sentAt;
    private boolean isDeleted;
    private int seenByCount;
}
