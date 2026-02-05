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

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** * [修改重點 1] 加入 uid 與 roles 到 JWT Claims
     * 修改參數為 Authentication 以便取得 CustomUserDetails 的詳細資訊
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

    /** * [修改重點 2] 提取所有 Claims
     * 方便 Filter 直接讀取 roles 而不需重複解析
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