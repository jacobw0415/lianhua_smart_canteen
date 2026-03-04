package com.lianhua.erp.service;

import com.lianhua.erp.dto.user.JwtResponse;

/**
 * Refresh Token 與 MFA 流程的協調服務。
 * - 發行/驗證/撤銷 Refresh Token
 * - 建立 MFA 待驗證階段、驗證通過後發放 JWT + Refresh Token
 * - MFA 綁定：產生密鑰、確認啟用
 */
public interface RefreshTokenService {

    /**
     * 為使用者建立一組 Refresh Token 並回傳明文（僅此次回傳，請妥善保存）。
     */
    String issueRefreshToken(Long userId);

    /**
     * 以 Refresh Token 換發新的 Access Token（與可選的 Refresh Token 輪替）。
     * 若 Token 無效或已撤銷/過期則拋出異常。
     */
    JwtResponse refreshAccessToken(String refreshToken);

    /**
     * 撤銷單一 Refresh Token（依明文 Token 雜湊查找）。
     */
    void revokeRefreshToken(String refreshToken);

    /**
     * 撤銷該使用者所有 Refresh Token（如登出時）。
     */
    void revokeAllForUser(Long userId);

    /**
     * 登入成功但需 MFA 時：建立待驗證階段，回傳 pendingToken。
     */
    String createMfaPending(Long userId);

    /**
     * 以 pendingToken + 6 碼驗證，通過後發放 JWT 與 Refresh Token，並刪除 pending 階段。
     */
    JwtResponse verifyMfaAndIssueTokens(String pendingToken, String code);
}
