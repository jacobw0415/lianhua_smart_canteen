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
        config.setAllowCredentials(true);

        // ✅ 明確列出允許的前端來源
        config.setAllowedOrigins(List.of(
                "http://localhost:5173", // React-Admin 本機開發
                "http://127.0.0.1:5173"
        ));

        // ✅ 設定允許的 HTTP 方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // ✅ 設定允許的標頭
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // ✅ 若有自訂標頭（例如 JWT Token）
        // config.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
