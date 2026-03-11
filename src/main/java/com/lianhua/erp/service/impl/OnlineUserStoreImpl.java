package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.user.OnlineUserDto;
import com.lianhua.erp.service.OnlineUserStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 記憶體儲存目前 WebSocket 連線對應的線上使用者。
 * 支援同一使用者多裝置（多 session），僅在最後一個 session 斷線時視為下線。
 */
@Service
public class OnlineUserStoreImpl implements OnlineUserStore {

    /** sessionId -> (userId, username, fullName, onlineAt) */
    private final Map<String, OnlineEntry> sessionToUser = new ConcurrentHashMap<>();
    /** userId -> Set of sessionIds（同一人多裝置） */
    private final Map<Long, Set<String>> userToSessions = new ConcurrentHashMap<>();

    @Override
    public void register(String sessionId, Long userId, String username, String fullName) {
        LocalDateTime now = LocalDateTime.now();
        sessionToUser.put(sessionId, new OnlineEntry(userId, username, fullName, now));
        userToSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    @Override
    public OnlineUserDto unregister(String sessionId) {
        OnlineEntry entry = sessionToUser.remove(sessionId);
        if (entry == null) {
            return null;
        }
        Set<String> sessions = userToSessions.get(entry.userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userToSessions.remove(entry.userId);
                return OnlineUserDto.builder()
                        .id(entry.userId)
                        .username(entry.username)
                        .fullName(entry.fullName)
                        .onlineAt(entry.onlineAt)
                        .build();
            }
        }
        return null;
    }

    @Override
    public OnlineUserDto unregisterByUserId(Long userId) {
        Set<String> sessions = userToSessions.remove(userId);
        if (sessions == null || sessions.isEmpty()) {
            return null;
        }
        OnlineEntry anyEntry = null;
        for (String sid : sessions) {
            OnlineEntry e = sessionToUser.remove(sid);
            if (e != null) {
                anyEntry = e;
            }
        }
        return anyEntry == null ? null : OnlineUserDto.builder()
                .id(anyEntry.userId)
                .username(anyEntry.username)
                .fullName(anyEntry.fullName)
                .onlineAt(anyEntry.onlineAt)
                .build();
    }

    @Override
    public List<OnlineUserDto> getOnlineUsers() {
        Set<Long> seen = new HashSet<>();
        List<OnlineUserDto> list = new ArrayList<>();
        for (OnlineEntry e : sessionToUser.values()) {
            if (seen.add(e.userId)) {
                list.add(OnlineUserDto.builder()
                        .id(e.userId)
                        .username(e.username)
                        .fullName(e.fullName)
                        .onlineAt(e.onlineAt)
                        .build());
            }
        }
        list.sort(Comparator.comparing(OnlineUserDto::getOnlineAt));
        return list;
    }

    @Override
    public Set<Long> getOnlineUserIds() {
        return new HashSet<>(userToSessions.keySet());
    }

    private record OnlineEntry(Long userId, String username, String fullName, LocalDateTime onlineAt) {}
}
