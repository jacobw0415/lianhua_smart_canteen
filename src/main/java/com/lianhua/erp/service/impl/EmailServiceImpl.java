package com.lianhua.erp.service.impl;

import com.lianhua.erp.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    // 🌿 這裡的 frontendUrl 僅作為其他一般頁面跳轉的參考，不再用於密碼重設連結
    @Value("${app.frontend.default-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:no-reply@lianhua.com}")
    private String fromEmail;

    /**
     * 發送密碼重設郵件
     * 修正點：接收完整的 resetLink，不再於內部自行拼接
     */
    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        // 直接使用傳入的 resetLink，它已經包含了正確的 IP 或 localhost
        String content = String.format(
                "<h3>您好：</h3>" +
                        "<p>我們收到了您的密碼重設請求。請點擊下方連結以設定新密碼：</p>" +
                        "<p><a href='%s' style='display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;'>點此重設密碼</a></p>" +
                        "<p>此連結將在 15 分鐘後過期。如果您沒有發起此請求，請忽略此郵件。</p>" +
                        "<p>如果按鈕無法點擊，請複製以下連結至瀏覽器：<br>%s</p>" +
                        "<hr><p style='font-size: 0.8em; color: gray;'>此為系統自動發送，請勿直接回覆。</p>",
                resetLink, resetLink
        );

        sendHtmlEmail(toEmail, "Lianhua ERP - 密碼重設請求", content);
    }

    @Async
    @Override
    public void sendHtmlEmail(String to, String subject, String content) {
        if (mailSender == null) {
            log.error("JavaMailSender 未配置，無法發送郵件。");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("郵件成功發送至: {}", to);
        } catch (MessagingException e) {
            log.error("郵件發送失敗: {}", e.getMessage());
        }
    }
}