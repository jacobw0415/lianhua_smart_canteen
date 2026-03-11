package com.lianhua.erp.service;

import com.lianhua.erp.dto.user.OnlineUserDto;

import java.util.List;

/**
 * 線上使用者狀態儲存（記憶體），供 WebSocket 連線/斷線時更新，
 * 並提供 REST 查詢目前線上使用者清單。
 */
public interface OnlineUserStore {

    /**
     * 註冊一筆 WebSocket 連線為「上線」。
     * 同一使用者多裝置會有多個 sessionId，僅在最後一個 session 斷線時視為下線。
     *
     * @param sessionId WebSocket session ID
     * @param userId    使用者 ID
     * @param username  帳號
     * @param fullName  全名
     */
    void register(String sessionId, Long userId, String username, String fullName);

    /**
     * 移除一筆連線；若該使用者已無其他 session，則會自線上列表移除（GET /api/users/online 不再顯示）。
     * 不論是否為最後一個 session，皆不廣播 OFFLINE（僅正式登出時才廣播）。
     *
     * @param sessionId WebSocket session ID
     * @return 若此 session 對應的使用者因此已無任何連線，回傳該使用者的 OnlineUserDto；否則 null（供內部使用，登出時不依此廣播）
     */
    OnlineUserDto unregister(String sessionId);

    /**
     * 依使用者 ID 移除該使用者所有 WebSocket 連線（用於正式登出）。
     *
     * @param userId 使用者 ID
     * @return 若該使用者曾在線上列表中，回傳其 DTO 供廣播 OFFLINE；否則 null
     */
    OnlineUserDto unregisterByUserId(Long userId);

    /**
     * 取得目前所有線上使用者（含自己），依上線時間排序。
     */
    List<OnlineUserDto> getOnlineUsers();

    /**
     * 取得目前線上使用者 ID 集合，供快速判斷是否在線。
     */
    java.util.Set<Long> getOnlineUserIds();
}
