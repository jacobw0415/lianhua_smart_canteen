package com.lianhua.erp.config;

import com.lianhua.erp.audit.ActivityAuditInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 註冊全系統活動稽核攔截器。
 */
@Configuration
@EnableConfigurationProperties(AuditProperties.class)
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ActivityAuditInterceptor activityAuditInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(activityAuditInterceptor)
                .addPathPatterns("/api/**");
    }
}
