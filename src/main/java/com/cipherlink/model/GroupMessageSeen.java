package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "group_message_seens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMessageSeen {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID groupMessageId;
    private UUID userId;

    @Builder.Default
    private LocalDateTime seenAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupMessageId", insertable = false, updatable = false)
    private GroupMessage groupMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;
}
