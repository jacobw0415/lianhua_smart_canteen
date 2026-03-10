package com.lianhua.erp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 簡易全域 API Rate Limiting Filter（單節點版）：
 * - 以 IP + 路徑為 key，在指定時間窗內限制最大請求數。
 * - 主要保護敏感端點，例如 /api/users/**、/api/roles/**、/api/permissions/**。
 *
 * 若未來需要多節點一致，可將資料結構改為 Redis 等分散式儲存。
 */
@Component
@Slf4j
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static class WindowInfo {
        int count;
        Instant windowStart;
    }

    private final Map<String, WindowInfo> windows = new ConcurrentHashMap<>();

    @Value("${security.api.window-seconds:60}")
    private long windowSeconds;

    @Value("${security.api.max-requests-per-window:60}")
    private int maxRequestsPerWindow;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!isProtectedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        String key = ip + "|" + path;

        WindowInfo info = windows.computeIfAbsent(key, k -> {
            WindowInfo w = new WindowInfo();
            w.windowStart = Instant.now();
            w.count = 0;
            return w;
        });

        Instant now = Instant.now();
        if (info.windowStart.plusSeconds(windowSeconds).isBefore(now)) {
            info.windowStart = now;
            info.count = 0;
        }

        info.count++;
        if (info.count > maxRequestsPerWindow) {
            log.warn("API rate limit exceeded for key {}: {} requests within {} seconds", key, info.count, windowSeconds);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Too many requests, please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedPath(String path) {
        if (path == null) return false;
        return path.startsWith("/api/users")
                || path.startsWith("/api/roles")
                || path.startsWith("/api/permissions")
                || path.startsWith("/api/auth/refresh");
    }
}

