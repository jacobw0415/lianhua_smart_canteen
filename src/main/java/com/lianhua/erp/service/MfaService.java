package com.lianhua.erp.service;

import com.lianhua.erp.dto.auth.MfaSetupResponse;


/**
 * MFA（TOTP）服務：產生密鑰、產生 QR 用 otpauth URL、驗證 6 碼。
 * 與 Google Authenticator / 其他 TOTP App 相容。
 */
public interface MfaService {

    /**
     * 產生一組 TOTP 密鑰（Base32）並組出 QR Code 用的 otpauth URL。
     *
     * @param issuer  應用名稱（如 Lianhua ERP）
     * @param username 使用者帳號，用於 otpauth 標籤
     */
    MfaSetupResponse generateSetup(String issuer, String username);

    /**
     * 驗證使用者輸入的 6 碼是否與密鑰當前時間窗相符。
     *
     * @param secretBase32 儲存於使用者的 TOTP 密鑰（Base32）
     * @param code         使用者輸入的 6 位數字
     * @return 是否驗證通過
     */
    boolean verifyCode(String secretBase32, String code);
}
