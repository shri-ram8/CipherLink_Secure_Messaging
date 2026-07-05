package com.cipherlink.repository;

import com.cipherlink.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    List<String> findFcmTokenByUserId(UUID userId);
    List<DeviceToken> findByUserId(UUID userId);
    Optional<DeviceToken> findByUserIdAndDeviceId(UUID userId, UUID deviceId);

    @Query("SELECT dt.fcmToken FROM DeviceToken dt WHERE dt.userId = :userId")
    List<String> findTokensByUserId(@Param("userId") UUID userId);
}
