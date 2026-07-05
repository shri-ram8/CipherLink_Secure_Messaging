package com.cipherlink.repository;

import com.cipherlink.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, UUID> {
    boolean existsByStoryIdAndViewerUserId(UUID storyId, UUID viewerUserId);
    List<StoryView> findByStoryIdOrderByViewedAtDesc(UUID storyId);
}
