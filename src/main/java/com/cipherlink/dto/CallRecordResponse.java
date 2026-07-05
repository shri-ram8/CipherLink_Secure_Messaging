package com.cipherlink.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CallRecordResponse {
    private UUID callId;
    private UUID callerId;
    private UUID receiverId;
    private String callType;
    private String status;
    private String roomId;
    private LocalDateTime startedAt;
    private LocalDateTime answeredAt;
    private LocalDateTime endedAt;
    private Integer durationSeconds;
    private String otherUserName;
    private String otherUserAvatarUrl;
    private boolean isIncoming;
}

