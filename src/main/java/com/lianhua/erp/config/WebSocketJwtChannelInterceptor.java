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
 * WebSocket 認證攔截器
 * 強化重點：
 * 1. 支援從 Header、Native Header、以及 Handshake Attributes (Query Params) 取得 Token。
 * 2. 增加 credentialsChangedAt 檢查，配合 AuthService.logout 實現強制踢除。
 * 3. 驗證失敗時返回 null，明確拒絕 STOMP CONNECT 請求。
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

        // 僅處理 CONNECT 階段的認證
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = getToken(accessor);

        // 1. 基礎校驗：Token 是否存在、格式、黑名單
        if (!StringUtils.hasText(token) || !jwtUtils.validateJwtToken(token) || tokenBlacklistService.isBlacklisted(token)) {
            log.warn("WebSocket CONNECT: 無效 JWT 或已被列入黑名單，拒絕連線");
            return null; // 拒絕發送該 Message，導致 CONNECT 失敗
        }

        try {
            Claims claims = jwtUtils.getClaimsFromJwtToken(token);
            String username = claims.getSubject();

            // 2. 獲取使用者 ID (優先從 Claims 取，若無則查庫)
            Long uid = null;
            Object uidClaim = claims.get("uid");
            if (uidClaim instanceof Number) {
                uid = ((Number) uidClaim).longValue();
            }

            // 3. 安全校驗：檢查憑證變動時間
            // 確保登出或改密碼後，舊 Token 立即失效
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                log.warn("WebSocket CONNECT: 找不到使用者 {}", username);
                return null;
            }

            if (user.getCredentialsChangedAt() != null) {
                long issuedAtSeconds = claims.getIssuedAt().getTime() / 1000;
                long changedAtSeconds = user.getCredentialsChangedAt()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toEpochSecond();

                if (issuedAtSeconds < changedAtSeconds) {
                    log.warn("WebSocket CONNECT: Token 簽發時間 ({}) 早於憑證更新時間 ({})，拒絕連線",
                            issuedAtSeconds, changedAtSeconds);
                    return null;
                }
            }

            if (uid == null) uid = user.getId();

            // 4. 解析權限並設定 Principal
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            if (roles == null) roles = List.of();
            var authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            CustomUserDetails userDetails = new CustomUserDetails(uid, username, "", true, authorities);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

            accessor.setUser(auth);
            log.debug("WebSocket CONNECT: 使用者 {} (id={}) 認證成功", username, uid);

        } catch (Exception e) {
            log.error("WebSocket CONNECT: 認證過程發生異常", e);
            return null;
        }

        return message;
    }

    /**
     * 多重來源提取 Token 邏輯
     */
    private String getToken(StompHeaderAccessor accessor) {
        // A. 標準 Authorization Header (SockJS 有時會擋，但在 STOMP Frame 內可行)
        List<String> auth = accessor.getNativeHeader("Authorization");
        if (auth != null && !auth.isEmpty()) {
            String v = auth.get(0);
            if (v != null && v.startsWith("Bearer ")) {
                return v.substring(7).trim();
            }
        }

        // B. 自定義 token Header
        auth = accessor.getNativeHeader("token");
        if (auth != null && !auth.isEmpty() && auth.get(0) != null) {
            return auth.get(0).trim();
        }

        // C. URL Query Params (由 WebSocketHandshakeHandler 存入)
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes != null && attributes.get("queryParams") instanceof Map<?, ?> params) {
            Object t = params.get("token");
            if (t instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
                return list.get(0).toString().trim();
            }
        }

        return null;
    }
}