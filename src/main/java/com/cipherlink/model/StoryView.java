package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "story_views")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryView {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID storyId;
    private UUID viewerUserId;

    @Builder.Default
    private LocalDateTime viewedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storyId", insertable = false, updatable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewerUserId", insertable = false, updatable = false)
    private User viewer;
}
