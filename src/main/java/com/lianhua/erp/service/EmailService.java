package com.lianhua.erp.service;

/**
 * 🌿 Email 服務介面
 * 定義系統郵件發送的行為契約，支援密碼重設與通知中心
 */
public interface EmailService {

    /**
     * 發送密碼重設郵件
     * @param toEmail 收件人地址
     * @param resetLink 完整的重設連結 (包含 Protocol, IP/Domain, Path 與 Token)
     */
    void sendPasswordResetEmail(String toEmail, String resetLink);

    /**
     * 通用 HTML 郵件發送 (供通知中心使用)
     * @param to 收件人地址
     * @param subject 郵件標題
     * @param content HTML 格式的內容
     */
    void sendHtmlEmail(String to, String subject, String content);
}