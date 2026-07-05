package com.cipherlink.repository;

import com.cipherlink.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenAndIsRevokedFalseAndExpiresAtAfter(String token, LocalDateTime now);
    List<RefreshToken> findByExpiresAtBeforeOrIsRevokedTrue(LocalDateTime now);
}
