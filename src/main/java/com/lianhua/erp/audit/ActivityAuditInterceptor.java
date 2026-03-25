package com.lianhua.erp.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lianhua.erp.config.AuditProperties;
import com.lianhua.erp.dto.audit.ActivityAuditRecordRequest;
import com.lianhua.erp.security.SecurityUtils;
import com.lianhua.erp.service.ActivityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 於請求完成後寫入活動稽核：含已登入使用者的變更與匯出（GET /export）。
 */
@Component
@RequiredArgsConstructor
public class ActivityAuditInterceptor implements HandlerInterceptor {

    private static final String ATTR_AUDIT_START_NANOS = "com.lianhua.erp.audit.startNanos";

    private final ActivityAuditService activityAuditService;
    private final AuditProperties auditProperties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        if (auditProperties.isEnabled()) {
            request.setAttribute(ATTR_AUDIT_START_NANOS, System.nanoTime());
        }
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {
        try {
            if (!auditProperties.isEnabled()) {
                return;
            }
            if (ex != null) {
                return;
            }
            int status = response.getStatus();
            if (status == 0 || status < 200 || status >= 400) {
                return;
            }
            Long operatorId = SecurityUtils.getCurrentUserIdOrNull();
            if (operatorId == null) {
                return;
            }
            String operatorUsername = SecurityUtils.getCurrentUsernameOrNull();
            String uri = request.getRequestURI();
            String pathOnly = ActivityAuditPathSupport.normalizedPath(request);
            if (shouldSkipUri(pathOnly)) {
                return;
            }
            if (!(handler instanceof HandlerMethod hm)) {
                return;
            }
            String method = request.getMethod();
            if (!shouldAuditHttpMethod(method, pathOnly)) {
                return;
            }
            String action = mapToAction(method, pathOnly);
            String resourceType = ActivityAuditPathSupport.inferResourceType(pathOnly);
            Long resourceId = ActivityAuditPathSupport.extractFirstNumericId(pathOnly);
            Map<String, Object> ctxExtras = ActivityAuditContext.drain();
            String details = buildDetails(hm, request, response, ctxExtras);

            ActivityAuditRecordRequest record = new ActivityAuditRecordRequest(
                    operatorId,
                    operatorUsername,
                    action,
                    resourceType,
                    resourceId,
                    method,
                    uri,
                    truncateQuery(request.getQueryString()),
                    clientIp(request),
                    truncateUa(request.getHeader("User-Agent")),
                    details
            );
            activityAuditService.recordAsync(record);
        } finally {
            ActivityAuditContext.remove();
        }
    }

    private static boolean shouldSkipUri(String pathWithoutQuery) {
        if ("/api/session/stream".equals(pathWithoutQuery)) {
            return true;
        }
        // 僅略過「分頁查詢」本身；/export 仍會被稽核
        return "/api/admin/activity-audit-logs".equals(pathWithoutQuery);
    }

    private static boolean shouldAuditHttpMethod(String method, String pathWithoutQuery) {
        return switch (method) {
            case "GET" -> pathWithoutQuery.contains("/export");
            case "POST" -> !isAnonymousAuthPost(pathWithoutQuery) && !pathWithoutQuery.contains("/search");
            case "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }

    /**
     * 登入前即完成之端點，不應有 operatorId；若日後變更為已登入仍會記錄。
     */
    private static boolean isAnonymousAuthPost(String pathWithoutQuery) {
        return "/api/auth/login".equals(pathWithoutQuery)
                || "/api/auth/register".equals(pathWithoutQuery)
                || "/api/auth/refresh".equals(pathWithoutQuery)
                || "/api/auth/forgot-password".equals(pathWithoutQuery)
                || "/api/auth/reset-password".equals(pathWithoutQuery);
    }

    private static String mapToAction(String httpMethod, String pathWithoutQuery) {
        if ("POST".equals(httpMethod) && "/api/auth/logout".equals(pathWithoutQuery)) {
            return "LOGOUT";
        }
        if ("GET".equals(httpMethod) && pathWithoutQuery.contains("/export")) {
            return "EXPORT";
        }
        return switch (httpMethod) {
            case "POST" -> "CREATE";
            case "PUT" -> "UPDATE";
            case "PATCH" -> "PATCH";
            case "DELETE" -> "DELETE";
            default -> "OTHER";
        };
    }

    private String buildDetails(
            HandlerMethod hm,
            HttpServletRequest request,
            HttpServletResponse response,
            Map<String, Object> ctxExtras
    ) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("handler", hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName());
            Object startObj = request.getAttribute(ATTR_AUDIT_START_NANOS);
            if (startObj instanceof Long startNanos) {
                long ms = (System.nanoTime() - startNanos) / 1_000_000L;
                map.put("durationMs", ms);
            }
            map.put("httpStatus", response.getStatus());
            String reqId = request.getHeader("X-Request-Id");
            if (reqId == null || reqId.isBlank()) {
                reqId = request.getHeader("X-Correlation-Id");
            }
            if (reqId != null && !reqId.isBlank()) {
                map.put("requestId", truncate(reqId, 128));
            }
            if (ctxExtras != null && !ctxExtras.isEmpty()) {
                for (Map.Entry<String, Object> e : ctxExtras.entrySet()) {
                    map.put("ctx." + e.getKey(), e.getValue());
                }
            }
            String json = objectMapper.writeValueAsString(map);
            int max = auditProperties.getDetailsMaxChars();
            if (max > 0 && json.length() > max) {
                return json.substring(0, max);
            }
            return json;
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static String truncateQuery(String query) {
        if (query == null) {
            return null;
        }
        return query.length() > 512 ? query.substring(0, 512) : query;
    }

    private static String truncateUa(String ua) {
        if (ua == null) {
            return null;
        }
        return ua.length() > 512 ? ua.substring(0, 512) : ua;
    }
}
