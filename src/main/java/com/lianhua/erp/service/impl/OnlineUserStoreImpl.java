package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.user.OnlineUserDto;
import com.lianhua.erp.service.OnlineUserStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OnlineUserStoreImpl implements OnlineUserStore {

    /** sessionId -> OnlineEntry */
    private final Map<String, OnlineEntry> sessionToUser = new ConcurrentHashMap<>();
    /** userId -> Set of sessionIds */
    private final Map<Long, Set<String>> userToSessions = new ConcurrentHashMap<>();

    @Override
    public void register(String sessionId, Long userId, String username, String fullName) {
        LocalDateTime now = LocalDateTime.now();
        sessionToUser.put(sessionId, new OnlineEntry(userId, username, fullName, now));
        userToSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        log.debug("[OnlineStore] 註冊 - User: {}, Session: {}, 總連線: {}", username, sessionId, sessionToUser.size());
    }

    @Override
    public OnlineUserDto unregister(String sessionId) {
        // 1. 先確認該 Session 是否真的存在，避免重複觸發
        OnlineEntry entry = sessionToUser.get(sessionId);
        if (entry == null) return null;

        Long userId = entry.userId();

        // 2. 原子移除：確保只移除屬於該 User 的特定 SessionID
        userToSessions.computeIfPresent(userId, (key, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });

        // 3. 物理移除映射
        sessionToUser.remove(sessionId);

        // 4. 【關鍵修正】檢查該 User 是否真的「完全沒有」任何 Session 了
        if (!userToSessions.containsKey(userId)) {
            log.info("[OnlineStore] 用戶 {} 已無任何連線，廣播離線", entry.username());
            return OnlineUserDto.builder()
                    .id(entry.userId())
                    .username(entry.username())
                    .fullName(entry.fullName())
                    .onlineAt(entry.onlineAt())
                    .build();
        }

        log.debug("[OnlineStore] 用戶 {} 仍有其他連線，不廣播離線", entry.username());
        return null;
    }

    @Override
    public OnlineUserDto unregisterByUserId(Long userId) {
        Set<String> sessions = userToSessions.remove(userId);
        if (sessions == null || sessions.isEmpty()) return null;

        OnlineEntry lastEntry = null;
        for (String sid : sessions) {
            OnlineEntry e = sessionToUser.remove(sid);
            if (e != null) lastEntry = e;
        }

        if (lastEntry != null) {
            log.info("[OnlineStore] 強制清理用戶 {} (清除 Session 數: {})", lastEntry.username(), sessions.size());
            return OnlineUserDto.builder()
                    .id(lastEntry.userId())
                    .username(lastEntry.username())
                    .fullName(lastEntry.fullName())
                    .onlineAt(lastEntry.onlineAt())
                    .build();
        }
        return null;
    }

    @Override
    public List<OnlineUserDto> getOnlineUsers() {
        return sessionToUser.values().stream()
                .collect(Collectors.toMap(
                        OnlineEntry::userId,
                        e -> OnlineUserDto.builder()
                                .id(e.userId())
                                .username(e.username())
                                .fullName(e.fullName())
                                .onlineAt(e.onlineAt())
                                .build(),
                        (existing, replacement) -> existing.getOnlineAt().isBefore(replacement.getOnlineAt()) ? existing : replacement
                ))
                .values().stream()
                .sorted(Comparator.comparing(OnlineUserDto::getOnlineAt))
                .collect(Collectors.toList());
    }

    @Override
    public Set<Long> getOnlineUserIds() {
        return Collections.unmodifiableSet(userToSessions.keySet());
    }

    private record OnlineEntry(Long userId, String username, String fullName, LocalDateTime onlineAt) {}
}