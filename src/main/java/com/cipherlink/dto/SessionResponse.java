package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResponse {
    private UUID sessionId;
    private UUID receiverDeviceId;
    private boolean isEstablished;
    private LocalDateTime createdAt;
}
