package com.lianhua.erp.service;

import com.lianhua.erp.dto.notification.NotificationResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    /**
     * ✨ 新增：發送系統層級的告警通知 (專供 Scheduler 使用)
     * 適合 4~9 項：無需 template，直接發送文字訊息給特定角色或全體管理員
     */
    void sendSystemAlert(String message, String alertType, List<Long> receiverIds);

    /**
     * 發送通知給多位使用者
     */
    void send(String templateCode, String targetType, Long targetId,
              Map<String, Object> payload, List<Long> receiverIds);

    /**
     * ✨ 新增：獲取分頁後的通知歷史清單 (含已讀與未讀)
     * 用於「通知中心」主頁面
     * * @param userId   使用者 ID
     * @param pageable 分頁與排序參數 (由 Controller 傳入)
     * @return 分頁後的 DTO 包裝
     */
    Page<NotificationResponseDto> getNotificationsPage(Long userId, Pageable pageable);

    /**
     * 獲取當前使用者的未讀清單 (用於 Header 下拉選單)
     */
    List<NotificationResponseDto> getUnreadList(Long userId);

    /**
     * 標記為已讀
     */
    void markAsRead(Long userNotificationId);

    /**
     * 獲取未讀總數 (用於顯示小紅點數字)
     */
    long getUnreadCount(Long userId);
}