package com.lianhua.erp.service;

import com.lianhua.erp.domain.BlacklistedToken;
import com.lianhua.erp.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public void blacklist(String token, Date expiresAt) {
        if (token == null || token.isBlank()) {
            return;
        }

        if (blacklistedTokenRepository.existsByToken(token)) {
            return;
        }

        Instant expiry = expiresAt != null ? expiresAt.toInstant() : Instant.now().plusSeconds(3600);

        BlacklistedToken entity = BlacklistedToken.builder()
                .token(token)
                .expiresAt(expiry)
                .build();

        blacklistedTokenRepository.save(entity);
        log.info("Token 已加入黑名單，將於 {} 到期", expiry);

        // 簡單清理：每次加入時順便刪除已過期項目（避免表過大）
        try {
            long deleted = blacklistedTokenRepository.deleteByExpiresAtBefore(Instant.now());
            if (deleted > 0) {
                log.debug("已清理過期黑名單 Token {} 筆", deleted);
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return blacklistedTokenRepository.existsByToken(token);
    }
}

