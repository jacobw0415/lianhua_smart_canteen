package com.lianhua.erp.service;

import com.lianhua.erp.domain.User;
import com.lianhua.erp.dto.auth.ForgotPasswordRequest;
import com.lianhua.erp.dto.auth.MfaSetupResponse;
import com.lianhua.erp.repository.UserRepository;
import com.lianhua.erp.security.JwtUtils;
import com.lianhua.erp.security.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private static final String MFA_ISSUER = "Lianhua ERP";

    @Value("${app.frontend.default-url:http://localhost:5173}")
    private String defaultFrontendUrl;

    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final MfaService mfaService;

    public AuthService(TokenBlacklistService tokenBlacklistService,
                       JwtUtils jwtUtils,
                       RefreshTokenService refreshTokenService,
                       UserRepository userRepository,
                       MfaService mfaService) {
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.mfaService = mfaService;
    }

    /**
     * 處理忘記密碼邏輯：產生 Token 並發送動態網址信件
     */
    public void processForgotPassword(ForgotPasswordRequest request) {
        // 1. 決定 Base URL：優先使用前端傳來的，若無則使用設定檔預設值
        String baseUrl = request.getResetLinkBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = defaultFrontendUrl;
            log.info("ForgotPassword: Using default frontend URL: {}", baseUrl);
        }

        // 2. 實務上會在這裡產生 Token 並組合重設連結，傳給 EmailService 寄送
        //    本範例僅記錄 log，不建立實際連結，也避免在 log 中輸出 Token 內容
        log.info("ForgotPassword: Generated reset token for {}", request.getEmail());

        // 4. 調用郵件服務發送信件 (實作略)
        // emailService.sendPasswordResetEmail(request.getEmail(), resetLink);
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

        // 2. 將 Access Token 加入黑名單，並撤銷該使用者所有 Refresh Token
        try {
            var claims = jwtUtils.getClaimsFromJwtToken(token);
            tokenBlacklistService.blacklist(token, claims.getExpiration());
            Object uid = claims.get("uid");
            if (uid instanceof Number) {
                refreshTokenService.revokeAllForUser(((Number) uid).longValue());
            }
            log.info("Logout: Token 已加入黑名單 (前 8 碼): {}", SensitiveDataMasker.maskToken(token));
        } catch (Exception e) {
            log.warn("Logout failed internally, but continuing: {}", e.getMessage());
        }
    }

    /**
     * 取得 MFA 綁定設定（密鑰 + otpauth URL），並將密鑰暫存於使用者，待驗證通過後才設為啟用。
     */
    public MfaSetupResponse mfaSetup(Long userId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在"));
        MfaSetupResponse setup = mfaService.generateSetup(MFA_ISSUER, user.getUsername());
        user.setMfaSecret(setup.getSecret());
        user.setMfaEnabled(false);
        userRepository.save(user);
        return setup;
    }

    /**
     * 以當前輸入的 6 碼確認啟用 MFA；通過後將 mfaEnabled 設為 true。
     */
    public void mfaConfirmEnable(Long userId, String code) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在"));
        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new IllegalStateException("請先呼叫 MFA 設定 API 取得密鑰");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            throw new IllegalArgumentException("驗證碼錯誤");
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    /**
     * 關閉該使用者的 MFA（須先驗證當前 TOTP 碼，通過後清除密鑰並設為未啟用）。
     */
    public void mfaDisable(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在"));
        if (!Boolean.TRUE.equals(user.getMfaEnabled()) || user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new IllegalStateException("此帳號尚未啟用 MFA");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            throw new IllegalArgumentException("驗證碼錯誤，無法關閉 MFA");
        }
        user.setMfaSecret(null);
        user.setMfaEnabled(false);
        userRepository.save(user);
    }
}