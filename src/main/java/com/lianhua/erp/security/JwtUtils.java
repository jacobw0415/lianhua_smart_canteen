package com.lianhua.erp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    // 建議在 application.properties 設定：lianhua.app.jwtSecret (至少 32 字元)
    @Value("${lianhua.app.jwtSecret:LianhuaERP_Secure_Secret_Key_2026_Standard}")
    private String jwtSecret;

    @Value("${lianhua.app.jwtExpirationMs:86400000}") // 預設 24 小時
    private int jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** 產生 Token：在 Login 成功後呼叫 */
    public String generateJwtToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 從 Token 中提取使用者帳號：用於 Filter 驗證 */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /** 驗證 Token 是否合法與過期 */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("無效的 JWT 簽名: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT Token 已過期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("不支援的 JWT Token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims 字串為空: {}", e.getMessage());
        }
        return false;
    }
}