package com.lianhua.erp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;

@Slf4j
@Component
public class JwtUtils {

    @Value("${lianhua.app.jwtSecret:LianhuaERP_Secure_Secret_Key_2026_Standard}")
    private String jwtSecret;

    @Value("${lianhua.app.jwtExpirationMs:3600000}")
    private int jwtExpirationMs;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    private SecretKey getSigningKey() {
        // 開發環境允許使用預設 Secret，但在非 dev profile 時若仍為預設值，直接阻止啟動以避免誤用。
        boolean looksLikeDefaultSecret = jwtSecret != null
                && jwtSecret.contains("LianhuaERP_Secure_Secret_Key_2026_Standard");
        boolean isDevProfile = activeProfiles != null && activeProfiles.contains("dev");

        if (looksLikeDefaultSecret) {
            if (isDevProfile) {
                log.warn("目前在 dev profile 正在使用預設 JWT Secret，請於正式環境設定 lianhua.app.jwtSecret 或環境變數 JWT_SECRET 以提高安全性。");
            } else {
                log.error("偵測到在非 dev profile 使用預設 JWT Secret，為避免安全風險系統將停止啟動。請設定 lianhua.app.jwtSecret 或環境變數 JWT_SECRET。");
                throw new IllegalStateException("JWT Secret 未正確設定，請參考 application.properties 中 lianhua.app.jwtSecret 說明。");
            }
        }

        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 產生 JWT，並將 uid 與 authorities 寫入 Claims。
     * 註：claim 名稱為 "roles"，但內容為「角色 + 權限」合併的 authority 清單（如 ROLE_ADMIN, user:view），
     * 供 Filter 還原為 Spring Security 的 GrantedAuthority，使 hasRole / hasAuthority 皆可正確運作。
     */
    public String generateJwtToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("uid", userPrincipal.getId())
                .claim("roles", roles)
                .setIssuer("Lianhua-ERP-System")
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 提取 JWT 所有 Claims（含 roles claim：角色與權限合併的 authority 清單）。
     * 方便 Filter 直接讀取並還原 Security 上下文，無需重複查庫。
     */
    public Claims getClaimsFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUserNameFromJwtToken(String token) {
        return getClaimsFromJwtToken(token).getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("無效的 JWT 簽名: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已過期，系統將強制登出使用者: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("不支援的 JWT Token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims 字串為空: {}", e.getMessage());
        }
        return false;
    }
}