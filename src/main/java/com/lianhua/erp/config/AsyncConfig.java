package com.lianhua.erp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 🔥 啟動非同步功能
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 設定核心線程數
        executor.setCorePoolSize(5);
        // 最大線程數
        executor.setMaxPoolSize(10);
        // 隊列容量
        executor.setQueueCapacity(500);
        // 線程名稱前綴
        executor.setThreadNamePrefix("Lianhua-Notify-");
        executor.initialize();
        return executor;
    }
}