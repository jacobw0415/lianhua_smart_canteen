package com.lianhua.erp.audit;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 由請求路徑推斷資源類型與路徑 ID（僅供稽核用，非嚴格 REST 語意）。
 */
public final class ActivityAuditPathSupport {

    private static final Pattern FIRST_ID = Pattern.compile("/(\\d+)(?:/|$)");

    private ActivityAuditPathSupport() {
    }

    public static String pathWithoutQuery(String uri) {
        if (uri == null) {
            return "";
        }
        int q = uri.indexOf('?');
        return q >= 0 ? uri.substring(0, q) : uri;
    }

    /**
     * 不含 context-path、不含 query 的 servlet 路徑（與 Spring MVC 對 /api/** 的匹配一致）。
     * 若應用程式設定了 {@code server.servlet.context-path}，請勿僅用 {@link HttpServletRequest#getRequestURI()} 做字串比對。
     */
    public static String normalizedPath(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        String servletPath = request.getServletPath();
        if (servletPath != null) {
            sb.append(servletPath);
        }
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            sb.append(pathInfo);
        }
        String path = sb.toString();
        if (!path.isEmpty()) {
            return path;
        }
        String uri = pathWithoutQuery(request.getRequestURI());
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            return uri.substring(ctx.length());
        }
        return uri;
    }

    /**
     * 取 /api/ 後第一個路徑段，轉成大寫 + 底線（order_customers → ORDER_CUSTOMERS）。
     */
    public static String inferResourceType(String uri) {
        String path = pathWithoutQuery(uri);
        if (!path.startsWith("/api/")) {
            return "UNKNOWN";
        }
        String rest = path.substring(5);
        int slash = rest.indexOf('/');
        String first = slash < 0 ? rest : rest.substring(0, slash);
        if (first.isEmpty()) {
            return "UNKNOWN";
        }
        return first.toUpperCase(Locale.ROOT).replace('-', '_');
    }

    public static Long extractFirstNumericId(String uri) {
        String path = pathWithoutQuery(uri);
        Matcher m = FIRST_ID.matcher(path);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
