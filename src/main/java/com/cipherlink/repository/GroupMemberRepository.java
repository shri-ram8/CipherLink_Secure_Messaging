package com.cipherlink.repository;

import com.cipherlink.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    Optional<GroupMember> findByGroupIdAndUserIdAndIsRemovedFalse(UUID groupId, UUID userId);
    boolean existsByGroupIdAndUserIdAndIsRemovedFalse(UUID groupId, UUID userId);
    long countByGroupIdAndIsRemovedFalse(UUID groupId);
    List<GroupMember> findByGroupIdAndIsRemovedFalse(UUID groupId);
    List<UUID> findUserIdByGroupIdAndIsRemovedFalse(UUID groupId);

    @Query("SELECT gm.userId FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.isRemoved = false")
    List<UUID> findMemberIds(@Param("groupId") UUID groupId);

    @Query("SELECT gm.groupId FROM GroupMember gm WHERE gm.userId = :userId AND gm.isRemoved = false")
    List<UUID> findGroupIdsByUserId(@Param("userId") UUID userId);
}
