package com.lianhua.erp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 活動稽核非同步寫入專用執行緒池（避免預設無上限執行緒耗盡資源）。
 * 參數見 {@code app.audit.async.*}。
 */
@Configuration
@RequiredArgsConstructor
public class AuditAsyncConfiguration {

    private final AuditProperties auditProperties;

    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        AuditProperties.Async cfg = auditProperties.getAsync();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, cfg.getCorePoolSize()));
        executor.setMaxPoolSize(Math.max(executor.getCorePoolSize(), cfg.getMaxPoolSize()));
        executor.setQueueCapacity(Math.max(1, cfg.getQueueCapacity()));
        String prefix = cfg.getThreadNamePrefix();
        executor.setThreadNamePrefix(prefix == null || prefix.isBlank() ? "activity-audit-" : prefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
