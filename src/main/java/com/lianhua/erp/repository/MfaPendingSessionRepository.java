package com.lianhua.erp.repository;

import com.lianhua.erp.domain.MfaPendingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface MfaPendingSessionRepository extends JpaRepository<MfaPendingSession, Long> {

    Optional<MfaPendingSession> findByTokenAndExpiresAtAfter(String token, Instant expiresAt);

    @Modifying
    @Query("DELETE FROM MfaPendingSession m WHERE m.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM MfaPendingSession m WHERE m.expiresAt < :before")
    int deleteByExpiresAtBefore(@Param("before") Instant before);

    Optional<Object> findByUserId(Long userId);
}
