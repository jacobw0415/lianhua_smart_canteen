package com.lianhua.erp.config;

import com.lianhua.erp.domain.User;
import com.lianhua.erp.repository.UserRepository;
import com.lianhua.erp.security.CustomUserDetails;
import com.lianhua.erp.security.JwtUtils;
import com.lianhua.erp.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WebSocket 連線時從 STOMP CONNECT 的 header 或 query 取得 JWT，驗證後設定 Principal。
 * 前端需在 CONNECT frame 帶入 token，或連線 URL 帶 query token=xxx。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = getToken(accessor);
        if (!StringUtils.hasText(token) || !jwtUtils.validateJwtToken(token) || tokenBlacklistService.isBlacklisted(token)) {
            log.debug("WebSocket CONNECT: 無有效 JWT，拒絕連線");
            return message;
        }

        try {
            Claims claims = jwtUtils.getClaimsFromJwtToken(token);
            String username = claims.getSubject();
            Long uid = null;
            Object uidClaim = claims.get("uid");
            if (uidClaim instanceof Number) {
                uid = ((Number) uidClaim).longValue();
            }
            if (uid == null || uid <= 0) {
                uid = userRepository.findByUsername(username).map(User::getId).orElse(0L);
            }
            if (uid <= 0) {
                return message;
            }

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            if (roles == null) roles = List.of();
            var authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

            CustomUserDetails userDetails = new CustomUserDetails(uid, username, "", true, authorities);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            accessor.setUser(auth);
            log.debug("WebSocket CONNECT: 已設定使用者 {} (id={})", username, uid);
        } catch (Exception e) {
            log.warn("WebSocket CONNECT: JWT 解析失敗 {}", e.getMessage());
        }
        return message;
    }

    private String getToken(StompHeaderAccessor accessor) {
        List<String> auth = accessor.getNativeHeader("Authorization");
        if (auth != null && !auth.isEmpty()) {
            String v = auth.get(0);
            if (v != null && v.startsWith("Bearer ")) {
                return v.substring(7).trim();
            }
        }
        auth = accessor.getNativeHeader("token");
        if (auth != null && !auth.isEmpty() && auth.get(0) != null) {
            return auth.get(0).trim();
        }
        Object q = accessor.getSessionAttributes() != null ? accessor.getSessionAttributes().get("queryParams") : null;
        if (q instanceof Map<?, ?> params) {
            Object t = params.get("token");
            if (t instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
                return list.get(0).toString().trim();
            }
        }
        return null;
    }
}
