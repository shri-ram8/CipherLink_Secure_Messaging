package com.cipherlink.repository;

import com.cipherlink.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {
    List<Contact> findByOwnerIdAndIsBlockedFalse(UUID ownerId);
    boolean existsByOwnerIdAndContactUserId(UUID ownerId, UUID contactUserId);
    Optional<Contact> findByOwnerIdAndContactUserId(UUID ownerId, UUID contactUserId);
    List<UUID> findContactUserIdByOwnerIdAndIsBlockedFalse(UUID ownerId);

    @Query("SELECT c.contactUserId FROM Contact c WHERE c.ownerId = :ownerId AND c.isBlocked = false")
    List<UUID> findContactIdsByOwnerId(@Param("ownerId") UUID ownerId);
}
