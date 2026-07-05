package com.cipherlink.repository;

import com.cipherlink.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface PreKeyRepository extends JpaRepository<PreKey, UUID> {
    Optional<PreKey> findFirstByDeviceIdAndIsUsedFalse(UUID deviceId);
    boolean existsByDeviceIdAndPreKeyId(UUID deviceId, int preKeyId);
    long countByDeviceIdAndIsUsedFalse(UUID deviceId);
}
