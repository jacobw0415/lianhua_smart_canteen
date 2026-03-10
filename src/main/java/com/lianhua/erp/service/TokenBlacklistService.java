package com.lianhua.erp.service;

import com.lianhua.erp.domain.BlacklistedToken;
import com.lianhua.erp.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("產生 Token 雜湊失敗，將直接略過黑名單寫入：{}", e.getMessage());
            return null;
        }
    }

    public void blacklist(String token, Date expiresAt) {
        if (token == null || token.isBlank()) {
            return;
        }

        String tokenHash = hash(token);
        if (tokenHash == null) {
            return;
        }

        if (blacklistedTokenRepository.existsByTokenHash(tokenHash)) {
            return;
        }

        Instant expiry = expiresAt != null ? expiresAt.toInstant() : Instant.now().plusSeconds(3600);

        BlacklistedToken entity = BlacklistedToken.builder()
                .tokenHash(tokenHash)
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
        String tokenHash = hash(token);
        if (tokenHash == null) {
            return false;
        }
        return blacklistedTokenRepository.existsByTokenHash(tokenHash);
    }
}

