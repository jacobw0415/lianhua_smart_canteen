package com.lianhua.erp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 管理每個 userId 對應的 SSE 連線，用於推送即時事件（例如 FORCE_LOGOUT）。
 */
@Component
@Slf4j
public class SseSessionService {

    // userId -> 多條 SSE 連線
    private final Map<Long, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    /**
     * 訂閱目前登入者的 SSE 流。
     */
    public SseEmitter subscribe(Long userId) {
        // 設定較長 timeout（例如 30 分鐘）；前端可在斷線後自動重連
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);

        emittersByUserId
                .computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(userId, emitter);
        });
        emitter.onError(e -> {
            log.debug("SSE error for user {}: {}", userId, e.getMessage());
            removeEmitter(userId, emitter);
        });

        try {
            // 發送一個初始化事件，讓前端知道連線已建立
            emitter.send(SseEmitter.event().name("INIT").data("connected"));
        } catch (IOException e) {
            log.warn("Failed to send INIT event for user {}: {}", userId, e.getMessage());
        }

        return emitter;
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emittersByUserId.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersByUserId.remove(userId);
            }
        }
    }

    /**
     * 對指定 userId 推送 FORCE_LOGOUT 事件。
     */
    public void sendForceLogout(Long userId) {
        List<SseEmitter> list = emittersByUserId.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("FORCE_LOGOUT")
                        .data("force-logout"));
            } catch (IOException e) {
                log.debug("Failed to send FORCE_LOGOUT to user {}: {}", userId, e.getMessage());
                emitter.completeWithError(e);
            }
        }
    }
}

