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

    /**
     * 發送通知 (保持不變，但建議未來將渲染後的內容直接存入資料庫以利搜尋)
     */
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
     * ✨ 新增：獲取分頁後的通知列表 (包含已讀與未讀)
     * 對接 React-Admin 的 List 頁面
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotificationsPage(Long userId, Pageable pageable) {
        // 使用 Spring Data JPA 的分頁查詢
        Page<UserNotification> userNotiPage = userNotificationRepo.findByUserId(userId, pageable);

        // 將 Entity 分頁轉換為 DTO 分頁
        return userNotiPage.map(this::convertToDto);
    }

    /**
     * 獲取未讀列表 (保持用於 Header 小紅點，不分頁)
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadList(Long userId) {
        List<UserNotification> unread = userNotificationRepo.findUnreadByUserId(userId);
        return unread.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 抽取公共轉換邏輯
     */
    private NotificationResponseDto convertToDto(UserNotification un) {
        Notification n = un.getNotification();
        NotificationResponseDto dto = new NotificationResponseDto();

        // 注意：這裡的 ID 應該回傳 user_notification 的 ID，因為標記已讀是針對「特定使用者的關聯」
        dto.setUserNotificationId(un.getId());
        dto.setTargetType(n.getTargetType());
        dto.setTargetId(n.getTargetId());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setRead(un.getIsRead());

        // 解析 Payload 並渲染文字標題與內容
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
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
            String purchaseNo = (String) payload.getOrDefault("purchaseNo", "未知");

            switch (code) {
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