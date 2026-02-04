package com.lianhua.erp.security;

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
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 認證過濾器 (效能優化版)
 * 負責攔截請求、從 Claims 提取權限並建立安全上下文，避免重複查詢資料庫
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. 從請求標頭提取 JWT
            String jwt = parseJwt(request);

            // 2. 驗證 Token 是否有效
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                // 3. 直接從 Token 取得 Claims (內含 uid 與 roles)
                Claims claims = jwtUtils.getClaimsFromJwtToken(jwt);
                String username = claims.getSubject();

                // 4. 將 Claims 中的 roles 映射為 Spring Security 的 Authorities
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // 5. 建立認證物件 (使用自定義 Principal 儲存 uid 以供後續使用)
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, // 這裡可視需求改為傳入自定義的 User 物件
                        null,
                        authorities
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. 存入 Security 上下文，讓 @PreAuthorize 生效
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("已從 Token 建立使用者 '{}' 的安全上下文, 角色: {}", username, roles);
            }
        } catch (Exception e) {
            log.error("無法設定使用者認證: {}", e.getMessage());
        }

        // 7. 繼續過濾器鏈
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}