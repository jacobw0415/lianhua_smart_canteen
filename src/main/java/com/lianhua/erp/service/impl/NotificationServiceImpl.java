// src/main/java/com/lianhua/erp/service/impl/NotificationServiceImpl.java
package com.lianhua.erp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lianhua.erp.domain.Notification;
import com.lianhua.erp.domain.UserNotification;
import com.lianhua.erp.dto.notification.NotificationResponseDto;
import com.lianhua.erp.repository.NotificationRepository;
import com.lianhua.erp.repository.UserNotificationRepository;
import com.lianhua.erp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserNotificationRepository userNotificationRepo;
    private final ObjectMapper objectMapper; // Spring Boot 內建

    @Override
    @Transactional
    public void send(String templateCode, String targetType, Long targetId,
                     Map<String, Object> payload, List<Long> receiverIds) {
        try {
            // 1. 建立通知主體
            Notification n = new Notification();
            n.setTemplateCode(templateCode);
            n.setTargetType(targetType);
            n.setTargetId(targetId);
            n.setPayload(objectMapper.writeValueAsString(payload)); // 轉為 JSON 字串
            notificationRepo.save(n);

            // 2. 分發給使用者
            List<UserNotification> userNotifications = receiverIds.stream().map(uid -> {
                UserNotification un = new UserNotification();
                un.setUserId(uid);
                un.setNotification(n);
                un.setIsRead(false);
                return un;
            }).collect(Collectors.toList());

            userNotificationRepo.saveAll(userNotifications);

            log.info("Notification sent: type={}, receivers={}", templateCode, receiverIds.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification payload", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadList(Long userId) {
        List<UserNotification> unread = userNotificationRepo.findUnreadByUserId(userId);

        return unread.stream().map(un -> {
            Notification n = un.getNotification();
            NotificationResponseDto dto = new NotificationResponseDto();
            dto.setUserNotificationId(un.getId());
            dto.setTargetType(n.getTargetType());
            dto.setTargetId(n.getTargetId());
            dto.setCreatedAt(n.getCreatedAt());
            dto.setRead(un.getIsRead());

            // 解析 Payload 並渲染文字
            renderText(dto, n.getTemplateCode(), n.getPayload());

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long userNotificationId) {
        userNotificationRepo.findById(userNotificationId).ifPresent(un -> {
            un.setIsRead(true);
            un.setReadAt(LocalDateTime.now());
            userNotificationRepo.save(un);
        });
    }

    @Override
    public long getUnreadCount(Long userId) {
        return userNotificationRepo.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 渲染文字：將 Template Code 轉換為可讀的標題與內容
     * 未來產品化後，這段邏輯可以改為從資料庫的 notification_templates 表讀取
     */
    private void renderText(NotificationResponseDto dto, String code, String payloadJson) {
        try {
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
            String purchaseNo = (String) payload.getOrDefault("purchaseNo", "未知");

            switch (code) {
                // 🔥 新增：處理進貨單建立
                case "PURCHASE_CREATED_ALERT":
                    dto.setTitle("✨ 新進貨單建立");
                    dto.setContent(String.format("已建立新進貨單 %s，請確認內容與後續付款。", purchaseNo));
                    break;

                case "ITEM_ADDED_ALERT":
                    dto.setTitle("📦 進貨明細更新");
                    dto.setContent(String.format("單號 %s 已新增明細項目。", purchaseNo));
                    break;

                case "PURCHASE_VOIDED":
                    dto.setTitle("🚫 採購單已作廢");
                    dto.setContent(String.format("單號 %s 已被作廢，原因：%s",
                            purchaseNo, payload.getOrDefault("reason", "無")));
                    break;

                case "LARGE_PURCHASE_ALERT":
                    dto.setTitle("⚠️ 大額採購預警");
                    dto.setContent(String.format("單號 %s 金額達 %s 需特別注意",
                            purchaseNo, payload.getOrDefault("amount", "0")));
                    break;

                default:
                    dto.setTitle("系統通知");
                    dto.setContent("您有一則新的訊息");
            }
        } catch (Exception e) {
            dto.setTitle("系統通知");
            dto.setContent("訊息解析錯誤");
        }
    }
}