package com.lianhua.erp.config;

import com.lianhua.erp.dto.user.OnlineUserDto;
import com.lianhua.erp.dto.user.UserOnlineEventDto;
import com.lianhua.erp.repository.UserRepository;
import com.lianhua.erp.security.CustomUserDetails;
import com.lianhua.erp.service.OnlineUserStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;

/**
 * 監聽 WebSocket 連線/斷線，更新線上使用者狀態。
 * 修復重點：
 * 1. 透過 unregister 回傳值判斷是否廣播 OFFLINE，解決登出/異常關閉後仍顯示在線的問題。
 * 2. 只有當用戶最後一個分頁/連線斷開時才發送離線消息，避免閒置誤判。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketConnectionListener {

    public static final String TOPIC_ONLINE_USERS = "/topic/online-users";

    private final OnlineUserStore onlineUserStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = accessor.getSessionId();
            Authentication auth = (Authentication) accessor.getUser();

            if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
                log.warn("WebSocket 連線無驗證資訊，sessionId={}", sessionId);
                return;
            }

            CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();

            // 取得顯示名稱（優先從 DB，否則用 Username）
            String fullName = userRepository.findById(user.getId())
                    .map(u -> u.getFullName() != null ? u.getFullName() : user.getUsername())
                    .orElse(user.getUsername());

            // 1. 登記連線
            onlineUserStore.register(sessionId, user.getId(), user.getUsername(), fullName);

            // 2. 廣播上線消息
            broadcastEvent("ONLINE", user.getId(), user.getUsername(), fullName);

            log.info("使用者上線廣播: {} (ID: {}), Session: {}", user.getUsername(), user.getId(), sessionId);
        } catch (Exception e) {
            log.error("處理 SessionConnectedEvent 錯誤", e);
        }
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = accessor.getSessionId();

            // 1. 核心修正：直接執行 unregister 並取得回傳值
            // 若回傳非 null，代表這已經是該使用者的最後一個連線，系統應該視為完全下線
            OnlineUserDto departedUser = onlineUserStore.unregister(sessionId);

            // 2. 判斷是否需要發送離線廣播
            if (departedUser != null) {
                broadcastEvent("OFFLINE", departedUser.getId(), departedUser.getUsername(), departedUser.getFullName());
                log.info("使用者 {} 已完全斷開所有連線，廣播離線消息", departedUser.getUsername());
            } else {
                // 如果回傳 null，表示 userToSessions 裡該用戶還有其他 sessionId 存在
                log.debug("Session {} 已斷開，但使用者仍有其他連線，不廣播離線", sessionId);
            }
        } catch (Exception e) {
            log.error("處理 SessionDisconnectEvent 錯誤", e);
        }
    }

    /**
     * 統一廣播邏輯
     */
    private void broadcastEvent(String eventType, Long userId, String username, String fullName) {
        UserOnlineEventDto payload = UserOnlineEventDto.builder()
                .eventType(eventType)
                .userId(userId)
                .username(username)
                .fullName(fullName)
                .at(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend(TOPIC_ONLINE_USERS, payload);
    }
}