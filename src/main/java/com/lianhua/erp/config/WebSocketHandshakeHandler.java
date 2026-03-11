package com.lianhua.erp.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 將 WebSocket 連線 URL 的 query 參數存入 session attributes，
 * 供 {@link WebSocketJwtChannelInterceptor} 在 CONNECT 時取得 token。
 * 前端連線時請使用：/ws?token=YOUR_JWT
 */
public class WebSocketHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(@NonNull ServerHttpRequest request,
                                      @NonNull WebSocketHandler wsHandler,
                                      @NonNull Map<String, Object> attributes) {
        URI uri = request.getURI();
        if (uri != null && uri.getQuery() != null) {
            Map<String, List<String>> queryParams = parseQuery(uri.getQuery());
            attributes.put("queryParams", queryParams);
        }
        return null;
    }

    private static Map<String, List<String>> parseQuery(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(query.split("&"))
                .map(s -> s.split("=", 2))
                .filter(p -> p.length >= 1 && !p[0].isBlank())
                .collect(Collectors.groupingBy(
                        p -> p[0].trim(),
                        Collectors.mapping(p -> p.length == 2 ? p[1].trim() : "", Collectors.toList())
                ));
    }
}
