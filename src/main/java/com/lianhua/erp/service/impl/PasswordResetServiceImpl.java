package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.PasswordResetToken;
import com.lianhua.erp.domain.User;
import com.lianhua.erp.dto.auth.ForgotPasswordRequest;
import com.lianhua.erp.dto.auth.ResetPasswordRequest;
import com.lianhua.erp.repository.PasswordResetTokenRepository;
import com.lianhua.erp.repository.UserRepository;
import com.lianhua.erp.service.EmailService;
import com.lianhua.erp.service.PasswordResetService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // 從 application.properties 讀取預設網址，若無則預設 localhost
    @Value("${app.frontend.default-url:http://localhost:5173}")
    private String defaultFrontendUrl;

    @Override
    @Transactional
    public void processForgotPassword(@Valid ForgotPasswordRequest request) {
        // 1. 查找 Email 是否存在
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("找不到與此 Email 關聯的帳號"));

        // 2. 清理該使用者舊的重設請求，確保 Token 唯一性
        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();

        // 3. 生成 15 分鐘有效的 Token 並儲存
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        tokenRepository.save(resetToken);

        // 4. 動態組裝重設連結
        // 優先使用前端傳入的 URL (例如 http://10.18.2.103:5173)，若無則 fallback 到設定檔
        String baseUrl = request.getResetLinkBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = defaultFrontendUrl;
        }

        // 去除結尾斜線並拼接路徑與 Token
        String cleanBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        String resetLink = cleanBaseUrl + "/reset-password?token=" + token;

        log.info("發送密碼重設郵件至: {}, 產生的連結: {}", user.getEmail(), resetLink);

        // 5. 發送郵件 (傳送完整的 resetLink)
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. 驗證 Token 合法性
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("無效或已過期的 Token"));

        // 2. 檢查是否過期
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Token 已過期，請重新申請");
        }

        // 3. 更新使用者密碼並加密
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 4. 刪除已使用的 Token，確保單次有效
        tokenRepository.delete(resetToken);
        log.info("使用者 {} 密碼重設成功", user.getUsername());
    }
}