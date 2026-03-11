package com.lianhua.erp.config;

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
 * - 連線：登記至線上列表並廣播 ONLINE 至 /topic/online-users。
 * - 斷線：僅自線上列表移除（不廣播 OFFLINE），僅正式登出時才由 AuthService 廣播 OFFLINE。
 * 使用 @EventListener 且未加 @Async，故為同步執行，無異步延遲。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketConnectionListener {

    public static final String TOPIC_ONLINE_USERS = "/topic/online-users";

    private final OnlineUserStore onlineUserStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    /** 同步處理：STOMP CONNECT 成功後立即觸發，無異步延遲 */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        log.info("SessionConnectedEvent 已觸發，開始處理上線狀態");
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = accessor.getSessionId();
            Authentication auth = (Authentication) accessor.getUser();
            if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
                log.warn("WebSocket 連線無已驗證使用者，sessionId={}，略過上線登記", sessionId);
                return;
            }
            CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
            String fullName = userRepository.findById(user.getId())
                    .map(u -> u.getFullName() != null ? u.getFullName() : user.getUsername())
                    .orElse(user.getUsername());
            onlineUserStore.register(sessionId, user.getId(), user.getUsername(), fullName);
            UserOnlineEventDto payload = UserOnlineEventDto.builder()
                    .eventType("ONLINE")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .fullName(fullName)
                    .at(LocalDateTime.now())
                    .build();
            messagingTemplate.convertAndSend(TOPIC_ONLINE_USERS, payload);
            log.info("使用者上線已登記並廣播: {} (id={}), sessionId={}", user.getUsername(), user.getId(), sessionId);
        } catch (Exception e) {
            log.error("處理 SessionConnectedEvent 時發生錯誤", e);
            throw e;
        }
    }

    /** 同步處理：連線關閉時僅自線上列表移除，不廣播 OFFLINE（僅正式登出時才廣播） */
    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        log.debug("SessionDisconnectEvent 已觸發，自線上列表移除 session（不廣播 OFFLINE）");
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = accessor.getSessionId();
            onlineUserStore.unregister(sessionId);
        } catch (Exception e) {
            log.error("處理 SessionDisconnectEvent 時發生錯誤", e);
            throw e;
        }
    }
}
