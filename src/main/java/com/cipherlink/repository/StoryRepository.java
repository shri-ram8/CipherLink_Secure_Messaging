package com.cipherlink.repository;

import com.cipherlink.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface StoryRepository extends JpaRepository<Story, UUID> {
    @Query("SELECT s FROM Story s WHERE s.userId IN :userIds AND s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByUsers(@Param("userIds") List<UUID> userIds, @Param("now") LocalDateTime now);

    List<Story> findByUserIdAndIsActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(UUID userId, LocalDateTime now);
    List<Story> findByExpiresAtBeforeAndIsActiveTrue(LocalDateTime now);
}
