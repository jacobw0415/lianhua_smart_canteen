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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void processForgotPassword(@Valid ForgotPasswordRequest request) {
        // 1. 查找 Email 是否存在
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("找不到與此 Email 關聯的帳號"));

        // 2. 清理舊的重設請求
        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();

        // 3. 生成 15 分鐘有效的 Token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));


        tokenRepository.save(resetToken);

        // 4. 發送郵件
        emailService.sendPasswordResetEmail(user.getEmail(), token);
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

        // 4. 刪除已使用的 Token
        tokenRepository.delete(resetToken);
    }
}