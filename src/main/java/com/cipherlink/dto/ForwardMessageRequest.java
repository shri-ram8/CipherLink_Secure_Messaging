package com.cipherlink.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class ForwardMessageRequest {
    private UUID messageId;
    private List<UUID> targetConversationIds;
    private List<UUID> targetGroupIds;
}

