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

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotificationsPage(Long userId, Pageable pageable) {
        Page<UserNotification> userNotiPage = userNotificationRepo.findByUserId(userId, pageable);
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
        dto.setCreatedAt(n.getCreatedAt());
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

    /**
     * 修改重點：確保內容分行顯示，金額去小數點並加千分位
     */
    private void renderText(NotificationResponseDto dto, String code, String payloadJson) {
        try {
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // 1. 取得基礎資料 (處理單號/費用別)
            String no = String.valueOf(payload.getOrDefault("no",
                    payload.getOrDefault("purchaseNo", "未知")));

            // 🚀 關鍵修正：處理原因為 null 或 "null" 的情況
            Object rawReason = payload.get("reason");
            String reason = (rawReason == null || "null".equals(String.valueOf(rawReason)) || String.valueOf(rawReason).trim().isEmpty())
                    ? "未提供原因"
                    : String.valueOf(rawReason);

            // 2. 格式化金額：去掉小數點並加入千分位
            String amountRaw = String.valueOf(payload.getOrDefault("amount", "0"));
            String amountFormatted = "0";
            try {
                BigDecimal bd = new BigDecimal(amountRaw);
                amountFormatted = String.format("%,d", bd.intValue());
            } catch (Exception e) {
                amountFormatted = amountRaw;
            }

            // 3. 根據代碼渲染 (改為 \n 多行排版)
            switch (code) {
                case "EXPENSE_VOID_ALERT":
                    dto.setTitle("🚫 費用單作廢警示");
                    // 🚀 格式：費用別、金額、原因 分行
                    dto.setContent(String.format("費用別：%s\n金額：NT$ %s\n原因：%s",
                            no, amountFormatted, reason));
                    break;

                case "PURCHASE_VOID_ALERT":
                    dto.setTitle("🚫 進貨單作廢警示");
                    dto.setContent(String.format("單號：%s\n金額：NT$ %s\n原因：%s",
                            no, amountFormatted, reason));
                    break;

                case "RECEIPT_VOID_ALERT":
                    dto.setTitle("🚫 收款單作廢警示");
                    dto.setContent(String.format("訂單：%s\n金額：NT$ %s\n原因：%s",
                            no, amountFormatted, reason));
                    break;

                case "PAYMENT_VOID_ALERT":
                    dto.setTitle("🚫 付款單作廢警示");
                    dto.setContent(String.format("單號：%s\n金額：NT$ %s\n原因：%s",
                            no, amountFormatted, reason));
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