package com.lianhua.erp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LoginAttemptService {

    private static class AttemptInfo {
        int count;
        Instant firstAttemptAt;
    }

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.block-seconds:900}")
    private long blockSeconds;

    public void recordFailure(String key) {
        AttemptInfo info = attempts.computeIfAbsent(key, k -> {
            AttemptInfo a = new AttemptInfo();
            a.firstAttemptAt = Instant.now();
            a.count = 0;
            return a;
        });
        info.count++;
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    public boolean isBlocked(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null) {
            return false;
        }

        Instant now = Instant.now();

        // 視窗已過期則重置
        if (info.firstAttemptAt.plusSeconds(blockSeconds).isBefore(now)) {
            attempts.remove(key);
            return false;
        }

        if (info.count >= maxAttempts) {
            log.warn("Login blocked for key {}: {} attempts within {} seconds", key, info.count, blockSeconds);
            return true;
        }

        return false;
    }
}

