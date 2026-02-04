package com.lianhua.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 1. 允許攜帶憑證（如 Cookie，雖然我們主要用 JWT，但開啟此項能增加未來擴充性）
        config.setAllowCredentials(true);

        // 2. 允許的前端來源：包含 Vite 預設埠號
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000" // 備用常見埠號
        ));

        // 3. 允許的方法：ERP 常見的增刪改查，包含必要的 PATCH 與 OPTIONS
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 4. 允許的標頭：設為 "*" 以兼容 React-Admin 或 Swagger 可能帶入的自定義標頭
        config.setAllowedHeaders(List.of("*"));

        // 5. 💡 關鍵新增：暴露標頭
        // 確保前端 JavaScript (如 axios/fetch) 能夠讀取到回應中的 Authorization 標頭
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        // 6. 設定預檢請求 (OPTIONS) 的有效時間 (1小時)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 對所有以 /api/ 開頭的路徑生效
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}