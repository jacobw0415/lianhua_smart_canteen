package com.lianhua.erp.security;

import com.lianhua.erp.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 認證過濾器 (效能優化版)
 * 負責攔截請求、從 JWT 的 "roles" claim 提取「角色與權限」(authorities) 並建立安全上下文，避免重複查詢資料庫。
 * 註：claim 名為 "roles"，內容實際為角色名 + 權限名合併清單，供 hasRole / hasAuthority 使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final com.lianhua.erp.service.TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. 從請求標頭提取 JWT
            String jwt = parseJwt(request);

            // 2. 驗證 Token 是否有效，且未在黑名單中
            if (jwt != null && jwtUtils.validateJwtToken(jwt) && !tokenBlacklistService.isBlacklisted(jwt)) {

                // 3. 從 Token 取得 Claims（含 uid 與 "roles"：角色+權限合併的 authority 清單）
                Claims claims = jwtUtils.getClaimsFromJwtToken(jwt);
                String username = claims.getSubject();

                // 4. 將 "roles" claim 映射為 Spring Security 的 Authorities（供 hasRole / hasAuthority 使用）
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);
                if (roles == null) {
                    roles = List.of();
                }

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // 5. 從 JWT 取得 uid，組裝 CustomUserDetails 作為 Principal（供 /api/users/me、通知中心等 API 取得 currentUserId）
                Long uid = null;
                Object uidClaim = claims.get("uid");
                if (uidClaim instanceof Number) {
                    uid = ((Number) uidClaim).longValue();
                }
                // 若 JWT 無 uid 或為 0（舊版 token 或異常），依 username 查庫補齊，並順便做 credentialsChangedAt 檢查
                var userOpt = userRepository.findByUsername(username);
                if (uid == null || uid <= 0L) {
                    uid = userOpt.map(u -> u.getId()).orElse(0L);
                    if (uid > 0L) {
                        log.debug("JWT 缺少有效 uid，已依 username '{}' 補齊為 {}", username, uid);
                    }
                }

                // 若使用者曾更新密碼或被強制登出，且 credentialsChangedAt 晚於 Token 簽發時間，則拒絕此 Token
                userOpt.ifPresent(user -> {
                    if (user.getCredentialsChangedAt() != null) {
                        Instant issuedAt = claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : null;
                        if (issuedAt != null && issuedAt.isBefore(user.getCredentialsChangedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())) {
                            throw new RuntimeException("Token 已因憑證更新而失效");
                        }
                    }
                });

                CustomUserDetails userDetails = new CustomUserDetails(
                        uid != null ? uid : 0L,
                        username,
                        "",
                        true,
                        authorities
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        authorities
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. 存入 Security 上下文，讓 @PreAuthorize 生效
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("已從 Token 建立使用者 '{}' 的安全上下文, authorities: {}", username, roles);
            }
        } catch (Exception e) {
            log.error("無法設定使用者認證: {}", e.getMessage());
        }

        // 7. 繼續過濾器鏈
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        // 優先從 Authorization Header 取得 Bearer Token（一般 REST API 使用）
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7).trim();
        }

        // 若無 Authorization，則嘗試從 query parameter 讀取 token（給 SSE 等長連線使用）
        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken.trim();
        }

        return null;
    }
}