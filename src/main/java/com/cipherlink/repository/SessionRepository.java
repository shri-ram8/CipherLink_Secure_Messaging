package com.cipherlink.repository;

import com.cipherlink.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findBySenderDeviceIdAndReceiverDeviceId(UUID senderDeviceId, UUID receiverDeviceId);
    boolean existsBySenderDeviceIdAndReceiverDeviceIdAndIsEstablishedTrue(UUID senderDeviceId, UUID receiverDeviceId);
}
