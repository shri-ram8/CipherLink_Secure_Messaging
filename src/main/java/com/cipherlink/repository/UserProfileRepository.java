package com.cipherlink.repository;

import com.cipherlink.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    List<UserProfile> findByUserIdIn(List<UUID> userIds);

    @Modifying
    @Query("UPDATE UserProfile p SET p.isOnline = :online, p.lastSeen = :now WHERE p.userId = :userId")
    void updateOnlineStatus(@Param("userId") UUID userId,
                            @Param("online") boolean online,
                            @Param("now") LocalDateTime now);
}
