package com.cipherlink.repository;

import com.cipherlink.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.conversationId = :convId AND m.isDeleted = false ORDER BY m.sentAt ASC")
    List<Message> findPagedMessages(@Param("convId") UUID convId, Pageable pageable);

    long countByConversationIdAndIsDeletedFalse(UUID conversationId);

    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.conversationId = :convId " +
            "AND m.isDeleted = false " +
            "AND m.senderDeviceId NOT IN :deviceIds " +
            "AND m.status <> com.cipherlink.model.MessageStatus.SEEN")
    long countUnreadMessages(@Param("convId") UUID convId, @Param("deviceIds") List<UUID> deviceIds);

    Optional<Message> findFirstByConversationIdAndIsDeletedFalseOrderBySentAtDesc(UUID conversationId);

    // Pinned messages for a conversation
    List<Message> findByConversationIdAndIsPinnedTrueAndIsDeletedFalseOrderByPinnedAtDesc(UUID conversationId);

    // Full-text search within a conversation
    @Query("SELECT m FROM Message m WHERE m.conversationId = :convId " +
            "AND m.isDeleted = false " +
            "AND LOWER(m.encryptedPayload) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY m.sentAt DESC")
    List<Message> searchMessages(@Param("convId") UUID convId, @Param("query") String query, Pageable pageable);

    // Mark all unseen messages in a conversation as seen
    @Modifying
    @Query("UPDATE Message m SET m.status = com.cipherlink.model.MessageStatus.SEEN " +
            "WHERE m.conversationId = :convId " +
            "AND m.senderDeviceId NOT IN :myDeviceIds " +
            "AND m.status <> com.cipherlink.model.MessageStatus.SEEN " +
            "AND m.isDeleted = false")
    void markConversationAsSeen(@Param("convId") UUID convId, @Param("myDeviceIds") List<UUID> myDeviceIds);
}
