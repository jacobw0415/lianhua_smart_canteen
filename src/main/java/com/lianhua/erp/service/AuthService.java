package com.lianhua.erp.service;

import com.lianhua.erp.dto.auth.ForgotPasswordRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    @Value("${app.frontend.default-url:http://localhost:5173}")
    private String defaultFrontendUrl;

    /**
     * 處理忘記密碼邏輯：產生 Token 並發送動態網址信件
     */
    public void processForgotPassword(ForgotPasswordRequest request) {
        // 1. 產生 Token (假設已有 tokenService)
        // String token = tokenService.createToken(request.getEmail());
        String token = "sample-token-123"; // 測試用佔位符

        // 2. 決定 Base URL：優先使用前端傳來的，若無則使用設定檔預設值
        String baseUrl = request.getResetLinkBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = defaultFrontendUrl;
            log.info("ForgotPassword: Using default frontend URL: {}", baseUrl);
        }

        // 3. 組合成完整重設連結 (處理結尾斜線並確保路徑正確)
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String resetLink = cleanBaseUrl + "/reset-password?token=" + token;

        log.info("ForgotPassword: Generated reset link for {}: {}", request.getEmail(), resetLink);

        // 4. 調用郵件服務發送信件 (實作略)
        // emailService.sendResetPasswordEmail(request.getEmail(), resetLink);
    }

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