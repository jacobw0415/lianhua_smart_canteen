package com.lianhua.erp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 全系統活動稽核開關與非同步寫入、details 長度等設定。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.audit")
public class AuditProperties {

    /**
     * 是否寫入 activity_audit_logs 並套用攔截器。
     */
    private boolean enabled = true;

    /**
     * 寫入資料庫前，details JSON 最大字元數（避免過大）。
     */
    private int detailsMaxChars = 8000;

    /**
     * 非同步寫入執行緒池設定。
     */
    private Async async = new Async();

    @Getter
    @Setter
    public static class Async {
        private int corePoolSize = 2;
        private int maxPoolSize = 16;
        private int queueCapacity = 2000;
        private String threadNamePrefix = "activity-audit-";
    }
}
