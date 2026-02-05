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
public class EmailServiceImpl implements EmailService { // 修正：應使用 implements 而非 extends

    // 🌿 若 properties 沒設定會導致 Bean 缺失，在此可搭配 @Autowired(required = false) 或確保 properties 已補齊
    private final JavaMailSender mailSender;

    // 🌿 從設定檔讀取前端網址，避免寫死 localhost，方便部署到雲端
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // 🌿 從設定檔讀取發件人，保持部署靈活性
    @Value("${spring.mail.username:no-reply@lianhua.com}")
    private String fromEmail;

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        // 使用配置的網址組成重設連結
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        String content = String.format(
                "<h3>您好：</h3>" +
                        "<p>我們收到了您的密碼重設請求。請點擊下方連結以設定新密碼：</p>" +
                        "<p><a href='%s' style='padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;'>點此重設密碼</a></p>" +
                        "<p>此連結將在 15 分鐘後過期。如果您沒有發起此請求，請忽略此郵件。</p>" +
                        "<hr><p style='font-size: 0.8em; color: gray;'>此為系統自動發送，請勿直接回覆。</p>",
                resetUrl
        );

        sendHtmlEmail(toEmail, "Lianhua ERP - 密碼重設請求", content);
    }

    @Async
    @Override
    public void sendHtmlEmail(String to, String subject, String content) {
        if (mailSender == null) {
            log.error("JavaMailSender 未配置，無法發送郵件。請檢查 application.properties 設定。");
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