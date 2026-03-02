package com.lianhua.erp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lianhua.erp.domain.Notification;
import com.lianhua.erp.domain.UserNotification;
import com.lianhua.erp.dto.notification.NotificationResponseDto;
import com.lianhua.erp.repository.NotificationRepository;
import com.lianhua.erp.repository.UserNotificationRepository;
import com.lianhua.erp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserNotificationRepository userNotificationRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void sendSystemAlert(String message, String alertType, List<Long> receiverIds) {
        // 將系統告警訊息包裝成 Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("alertType", alertType); // 例如: WARNING, ERROR, INFO

        // 呼叫原本的 send 方法，使用專屬的 TemplateCode
        // targetType 標記為 SYSTEM，targetId 設為 0L 表示這不是針對單一單據的動作
        this.send("SYSTEM_CHECK_ALERT", "SYSTEM", 0L, payload, receiverIds);
    }

    @Override
    @Transactional
    public void send(String templateCode, String targetType, Long targetId,
                     Map<String, Object> payload, List<Long> receiverIds) {
        try {
            Notification n = new Notification();
            n.setTemplateCode(templateCode);
            n.setTargetType(targetType);
            n.setTargetId(targetId);
            n.setPayload(objectMapper.writeValueAsString(payload));
            notificationRepo.save(n);

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

    /**
     * 修正重點：更換查詢方法以支援跨表 (notification.createdAt) 排序
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotificationsPage(Long userId, Pageable pageable) {
        // 使用帶有 JOIN FETCH 的自定義查詢方法，確保 JPA 能正確解析 notification 關聯表的排序欄位
        Page<UserNotification> userNotiPage = userNotificationRepo.findAllByUserIdWithFetch(userId, pageable);
        return userNotiPage.map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadList(Long userId) {
        List<UserNotification> unread = userNotificationRepo.findUnreadByUserId(userId);
        return unread.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private NotificationResponseDto convertToDto(UserNotification un) {
        Notification n = un.getNotification();
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setUserNotificationId(un.getId());
        dto.setTargetType(n.getTargetType());
        dto.setTargetId(n.getTargetId());
        dto.setCreatedAt(n.getCreatedAt()); // 這裡的時間現在可以被正確排序了
        dto.setRead(un.getIsRead());
        renderText(dto, n.getTemplateCode(), n.getPayload());
        return dto;
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

    private void renderText(NotificationResponseDto dto, String code, String payloadJson) {
        try {
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
            String no = String.valueOf(payload.getOrDefault("no", payload.getOrDefault("purchaseNo", "未知")));

            // --- 處理系統檢查通知 (4-9項) ---
            if ("SYSTEM_CHECK_ALERT".equals(code)) {
                String alertType = String.valueOf(payload.getOrDefault("alertType", "WARNING"));
                String message = String.valueOf(payload.getOrDefault("message", "系統檢查異常"));

                // 根據告警等級給予不同圖示
                String icon = "ERROR".equals(alertType) ? "🚨" : "💡";
                dto.setTitle(icon + " 系統檢查提醒");
                dto.setContent(message);
                return; // 處理完畢直接回傳，不走後面的單據解析邏輯
            }

            Object rawReason = payload.get("reason");
            String reason = (rawReason == null || "null".equals(String.valueOf(rawReason)) || String.valueOf(rawReason).trim().isEmpty())
                    ? "未提供原因" : String.valueOf(rawReason);

            String amountRaw = String.valueOf(payload.getOrDefault("amount", "0"));
            String amountFormatted;
            try {
                BigDecimal bd = new BigDecimal(amountRaw);
                amountFormatted = String.format("%,d", bd.intValue());
            } catch (Exception e) {
                amountFormatted = amountRaw;
            }

            switch (code) {
                case "EXPENSE_VOID_ALERT":
                    dto.setTitle("🚫 費用單作廢警示");
                    dto.setContent(String.format("費用別：%s\n金額：NT$ %s\n原因：%s", no, amountFormatted, reason));
                    break;
                case "PURCHASE_VOID_ALERT":
                    dto.setTitle("🚫 進貨單作廢警示");
                    dto.setContent(String.format("單號：%s\n金額：NT$ %s\n原因：%s", no, amountFormatted, reason));
                    break;
                case "RECEIPT_VOID_ALERT":
                    dto.setTitle("🚫 收款單作廢警示");
                    dto.setContent(String.format("訂單：%s\n金額：NT$ %s\n原因：%s", no, amountFormatted, reason));
                    break;
                case "PAYMENT_VOID_ALERT":
                    dto.setTitle("🚫 付款單作廢警示");
                    dto.setContent(String.format("單號：%s\n金額：NT$ %s\n原因：%s", no, amountFormatted, reason));
                    break;
                default:
                    dto.setTitle("財務系統通知");
                    dto.setContent(String.format("單號：%s\n狀態：已更新 (%s)", no, code));
            }
        } catch (Exception e) {
            log.error("Render text error: {}", e.getMessage());
            dto.setTitle("系統通知");
            dto.setContent("訊息內容解析異常");
        }
    }
}