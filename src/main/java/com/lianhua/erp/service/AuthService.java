package com.lianhua.erp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    /**
     * 執行登出邏輯：盡力清除狀態，失敗也不拋出異常
     */
    public void logout(String authHeader) {
        // 1. 基本檢查：無 Token 視為已登出
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("Logout: No valid Bearer token provided.");
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) return;

        // 2. 業務邏輯防護：DB 或黑名單操作
        try {
            // TODO: 未來可在此實作 Redis 黑名單標記
            log.info("Token processed for logout: {}", token.substring(0, 5) + "...");
        } catch (Exception e) {
            // 3. 捕捉所有例外，僅記錄日誌，防止回傳 500
            log.warn("Logout failed internally, but continuing: {}", e.getMessage());
        }
    }
}