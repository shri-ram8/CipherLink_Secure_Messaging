package com.cipherlink.repository;

import com.cipherlink.model.LiveLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveLocationRepository extends JpaRepository<LiveLocation, UUID> {

    Optional<LiveLocation> findFirstByUserIdAndConversationIdAndIsActiveTrueOrderBySharedAtDesc(
            UUID userId, UUID conversationId);

    List<LiveLocation> findByConversationIdAndIsActiveTrueAndExpiresAtAfter(
            UUID conversationId, LocalDateTime now);

    List<LiveLocation> findByGroupIdAndIsActiveTrueAndExpiresAtAfter(
            UUID groupId, LocalDateTime now);

    // Cleanup expired locations
    @Query("SELECT l FROM LiveLocation l WHERE l.isActive = true AND l.expiresAt < :now")
    List<LiveLocation> findExpired(@Param("now") LocalDateTime now);
}
