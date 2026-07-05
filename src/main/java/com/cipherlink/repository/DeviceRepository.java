package com.cipherlink.repository;

import com.cipherlink.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findByUserIdAndIsRevokedFalse(UUID userId);
    Optional<Device> findByIdAndUserIdAndIsRevokedFalse(UUID id, UUID userId);
    Optional<Device> findFirstByUserIdAndIsRevokedFalseOrderByCreatedAtDesc(UUID userId);
    List<Device> findByUserId(UUID userId);
}
