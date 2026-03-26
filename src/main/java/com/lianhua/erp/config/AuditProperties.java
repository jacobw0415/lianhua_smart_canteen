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

    /**
     * 活動稽核保留期：歸檔後刪除（例如保留 180 天）。
     */
    private Retention retention = new Retention();

    @Getter
    @Setter
    public static class Async {
        private int corePoolSize = 2;
        private int maxPoolSize = 16;
        private int queueCapacity = 2000;
        private String threadNamePrefix = "activity-audit-";
    }

    @Getter
    @Setter
    public static class Retention {
        /**
         * 是否啟用活動稽核歸檔/清理排程。
         */
        private boolean enabled = true;

        /**
         * 保留天數（過期後會歸檔再刪除）。
         */
        private int days = 180;

        /**
         * 每批搬移筆數，避免一次鎖表過久。
         */
        private int batchSize = 2000;

        /**
         * 排程 Cron：預設每天 02:00 執行一次。
         */
        private String cron = "0 0 2 * * *";
    }
}
