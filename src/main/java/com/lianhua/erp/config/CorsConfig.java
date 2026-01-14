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

        // ✅ 允許的前端來源
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        // ✅ 關鍵修正：加入 "PATCH" 方法
        // 瀏覽器在發送 PATCH 前會先發送 OPTIONS，這裡必須包含兩者
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // ✅ 關鍵修正：允許所有 Headers 或明確指定常用標頭
        // 有時候前端會帶入自訂標頭，設定為 "*" 最為穩健
        config.setAllowedHeaders(List.of("*"));

        // ✅ 設定預檢請求的有效時間 (秒)
        // 設定為 3600 秒 (1小時) 可以讓瀏覽器不用每次點擊都問一次 OPTIONS，增加即時性
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 確保路徑對應您的 API 根路徑
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}