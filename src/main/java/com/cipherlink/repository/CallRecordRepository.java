package com.cipherlink.repository;

import com.cipherlink.model.CallRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CallRecordRepository extends JpaRepository<CallRecord, UUID> {

    @Query("SELECT c FROM CallRecord c WHERE (c.callerId = :userId OR c.receiverId = :userId) ORDER BY c.startedAt DESC")
    List<CallRecord> findCallHistory(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM CallRecord c WHERE c.receiverId = :userId AND c.status = 'MISSED'")
    long countMissedCalls(@Param("userId") UUID userId);
}
