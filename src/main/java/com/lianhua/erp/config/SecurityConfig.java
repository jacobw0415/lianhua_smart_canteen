package com.lianhua.erp.config;

import com.lianhua.erp.security.ApiRateLimitFilter;
import com.lianhua.erp.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiRateLimitFilter apiRateLimitFilter;
    private final Environment environment;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String corsAllowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ApiRateLimitFilter apiRateLimitFilter,
                          Environment environment) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiRateLimitFilter = apiRateLimitFilter;
        this.environment = environment;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean isDevProfile = environment.acceptsProfiles(Profiles.of("dev"));

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 一般性安全標頭設定（大多在瀏覽器端生效）
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                // 開發環境：放寬給 Swagger UI 使用 inline style / data:image SVG 等
                                .policyDirectives(
                                        isDevProfile
                                                ? "default-src 'self'; " +
                                                  "script-src 'self'; " +
                                                  "style-src 'self' 'unsafe-inline'; " +
                                                  "img-src 'self' data:; " +
                                                  "frame-ancestors 'none'; " +
                                                  "object-src 'none';"
                                                : "default-src 'self'; " +
                                                  "script-src 'self'; " +
                                                  "style-src 'self'; " +
                                                  "img-src 'self'; " +
                                                  "frame-ancestors 'none'; " +
                                                  "object-src 'none';"
                                ))
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                )

                // ⭐ 啟用 CORS（搭配底下的 corsConfigurationSource）
                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/login", "/api/auth/register",
                                    "/api/auth/forgot-password", "/api/auth/reset-password",
                                    "/api/auth/refresh", "/api/auth/mfa/verify", "/api/auth/logout")
                            .permitAll();

                    // Swagger / OpenAPI：開發環境全開放，正式環境僅管理者可看，避免對外暴露完整 API 結構
                    if (isDevProfile) {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                .permitAll();
                    } else {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                .hasRole("ADMIN");
                    }

                    auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll();
                    auth.anyRequest().authenticated();
                })
                // 先在 UsernamePasswordAuthenticationFilter 之前套用 API Rate Limit
                .addFilterBefore(apiRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                // 再在同一位置之前套用 JWT 驗證 Filter（順序由註冊先後決定）
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ⭐ 全域 CORS 設定：允許設定檔中指定的前端來源存取 API
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new org.springframework.web.cors.CorsConfiguration();

        java.util.List<String> origins = java.util.Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (origins.isEmpty()) {
            origins = java.util.List.of("http://localhost:5173");
        }
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 🌿 建議：允許所有 Header，避免搜尋時因特定的 X-Total-Count 或分頁 Header 被擋
        configuration.setAllowedHeaders(java.util.List.of("*"));

        // 🌿 允許前端讀取 Response 中的自定義 Header (如分頁資訊)
        configuration.setExposedHeaders(java.util.List.of("X-Total-Count", "Content-Disposition"));

        configuration.setAllowCredentials(true);

        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}