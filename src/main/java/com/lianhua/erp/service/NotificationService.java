package com.lianhua.erp.service;

import com.lianhua.erp.dto.notification.NotificationResponseDto;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    /**
     * 發送通知給多位使用者
     */
    void send(String templateCode, String targetType, Long targetId,
              Map<String, Object> payload, List<Long> receiverIds);

    /**
     * 獲取當前使用者的未讀清單 (含文字渲染)
     */
    List<NotificationResponseDto> getUnreadList(Long userId);

    /**
     * 標記為已讀
     */
    void markAsRead(Long userNotificationId);

    /**
     * 獲取未讀總數
     */
    long getUnreadCount(Long userId);
}