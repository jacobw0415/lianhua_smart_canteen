package com.lianhua.erp.service;

import com.lianhua.erp.config.WebSocketConnectionListener;
import com.lianhua.erp.domain.User;
import com.lianhua.erp.dto.auth.ForgotPasswordRequest;
import com.lianhua.erp.dto.auth.MfaSetupResponse;
import com.lianhua.erp.dto.user.OnlineUserDto;
import com.lianhua.erp.dto.user.UserOnlineEventDto;
import com.lianhua.erp.repository.UserRepository;
import com.lianhua.erp.security.CustomUserDetails;
import com.lianhua.erp.security.EncryptionService;
import com.lianhua.erp.security.JwtUtils;
import com.lianhua.erp.security.SensitiveDataMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String MFA_ISSUER = "Lianhua ERP";

    @Value("${app.frontend.default-url:http://localhost:5173}")
    private String defaultFrontendUrl;

    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final MfaService mfaService;
    private final EncryptionService encryptionService;
    private final OnlineUserStore onlineUserStore;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 執行登出邏輯：強化版
     * 解決手機刷新後 Token 可能失效導致 userId 為空的連鎖問題
     */
    public void logout(String authHeader) {
        Long userId = null;
        String token = null;

        // 1. 嘗試從 Header 解析 Token 並獲取 userId
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
            try {
                var claims = jwtUtils.getClaimsFromJwtToken(token);
                tokenBlacklistService.blacklist(token, claims.getExpiration());
                Object uid = claims.get("uid");
                if (uid instanceof Number) {
                    userId = ((Number) uid).longValue();
                }
            } catch (Exception e) {
                log.warn("Logout: Token 解析失敗 (可能已過期)，將嘗試從 SecurityContext 獲取身份");
            }
        }

        // 2. 【核心救援】如果 Header 沒抓到 ID，從當前安全上下文 (SecurityContext) 抓取
        // 這是解決手機刷新後、Token 臨界點登出失效的關鍵
        if (userId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
                userId = details.getId();
                log.info("Logout: 透過 SecurityContext 救援成功，UserID: {}", userId);
            }
        }

        // 3. 執行物理清理
        if (userId != null) {
            executePhysicalLogout(userId);
        } else {
            log.warn("Logout: 完全無法識別使用者身份，放棄清理。");
        }

        if (token != null) {
            log.info("Logout: Token 處理完成: {}", SensitiveDataMasker.maskToken(token));
        }
    }

    /**
     * 執行實體的狀態清理與強制廣播
     */
    private void executePhysicalLogout(Long userId) {
        try {
            // 1. 撤銷所有 Refresh Token
            refreshTokenService.revokeAllForUser(userId);

            // 2. 更新憑證時間 (使所有舊連線在 Interceptor 層級被拒絕)
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setCredentialsChangedAt(LocalDateTime.now());
                userRepository.save(user);

                // 3. 清理記憶體 Store (移除所有殘留 Session)
                onlineUserStore.unregisterByUserId(userId);

                // 4. 【終極廣播】不檢查 removed 狀態，只要登出就強制發送離線訊號
                log.info("【廣播測試】準備發送離線訊號給用戶: {} (ID: {})", user.getUsername(), userId);
                broadcastOffline(user.getId(), user.getUsername(), user.getFullName());
            }
        } catch (Exception e) {
            log.error("AuthService: 清理用戶 {} 狀態時發生錯誤", userId, e);
        }
    }

    private void broadcastOffline(Long userId, String username, String fullName) {
        UserOnlineEventDto payload = UserOnlineEventDto.builder()
                .eventType("OFFLINE")
                .userId(userId)
                .username(username)
                .fullName(fullName)
                .at(LocalDateTime.now())
                .build();

        // 確保 Topic 路徑與前端訂閱完全一致
        messagingTemplate.convertAndSend(WebSocketConnectionListener.TOPIC_ONLINE_USERS, payload);
    }

    // --- 以下為 MFA 相關邏輯，保持不變 ---

    public void processForgotPassword(ForgotPasswordRequest request) {
        String baseUrl = request.getResetLinkBaseUrl() != null ? request.getResetLinkBaseUrl() : defaultFrontendUrl;
        log.info("ForgotPassword: Reset link generated for {}", request.getEmail());
    }

    public MfaSetupResponse mfaSetup(Long userId) {
        User user = userRepository.findByIdWithRoles(userId).orElseThrow(() -> new IllegalArgumentException("使用者不存在"));
        MfaSetupResponse setup = mfaService.generateSetup(MFA_ISSUER, user.getUsername());
        user.setMfaSecret(encryptionService.encrypt(setup.getSecret()));
        user.setMfaEnabled(false);
        userRepository.save(user);
        return setup;
    }

    @Transactional
    public void mfaConfirmEnable(Long userId, String code) {
        User user = userRepository.findByIdWithRoles(userId).orElseThrow(() -> new IllegalArgumentException("使用者不存在"));
        String decrypted = encryptionService.decrypt(user.getMfaSecret());
        if (decrypted == null || !mfaService.verifyCode(decrypted, code)) throw new IllegalArgumentException("驗證碼錯誤");
        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void mfaDisable(Long userId, String code) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("使用者不存在"));
        String decrypted = encryptionService.decrypt(user.getMfaSecret());
        if (decrypted == null || !mfaService.verifyCode(decrypted, code)) throw new IllegalArgumentException("驗證碼錯誤");
        user.setMfaSecret(null);
        user.setMfaEnabled(false);
        userRepository.save(user);
    }
}