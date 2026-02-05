package com.lianhua.erp.service;

import com.lianhua.erp.dto.auth.ForgotPasswordRequest;
import com.lianhua.erp.dto.auth.ResetPasswordRequest;
import jakarta.validation.Valid;

public interface PasswordResetService {
    // 發起重設請求並發送郵件
    void processForgotPassword(@Valid ForgotPasswordRequest request);

    // 驗證 Token 並更新密碼
    void resetPassword(ResetPasswordRequest request);
}