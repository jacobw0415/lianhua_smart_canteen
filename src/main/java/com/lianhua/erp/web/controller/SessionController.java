package com.lianhua.erp.web.controller;

import com.lianhua.erp.security.JwtUtils;
import com.lianhua.erp.security.SseSessionService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 提供 SSE 連線，用於推送即時事件（例如強制登出）。
 *
 * 前端呼叫方式（示意）：
 *   const es = new EventSource('/api/session/stream?token=' + accessToken);
 *   es.addEventListener('FORCE_LOGOUT', () => { // 清除 token 並導回登入頁 });
 */
@RestController
@RequestMapping("/api/session")
@Tag(name = "即時會話", description = "提供 SSE 連線以接收強制登出等即時事件")
@RequiredArgsConstructor
public class SessionController {

    private final JwtUtils jwtUtils;
    private final SseSessionService sseSessionService;

    @Operation(summary = "訂閱會話事件（SSE）", description = "登入後使用 access token 建立 SSE 連線，當被強制登出時會收到 FORCE_LOGOUT 事件。")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("token") String token, HttpServletResponse response) {
        // 簡單驗證 JWT；無效或過期則回傳 401
        if (token == null || token.isBlank() || !jwtUtils.validateJwtToken(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }

        Claims claims = jwtUtils.getClaimsFromJwtToken(token);
        Object uidClaim = claims.get("uid");
        if (!(uidClaim instanceof Number)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }

        Long userId = ((Number) uidClaim).longValue();
        return sseSessionService.subscribe(userId);
    }
}

