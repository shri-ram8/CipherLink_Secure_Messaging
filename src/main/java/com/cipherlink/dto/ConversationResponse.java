package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {
    private UUID id;
    private UUID otherUserId;
    private String otherUserPhone;
    private String otherUserName;
    private String otherUserAvatarUrl;
    private String lastMessage;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private int unreadCount;
    private boolean isOnline;
    private LocalDateTime lastSeen;
}
