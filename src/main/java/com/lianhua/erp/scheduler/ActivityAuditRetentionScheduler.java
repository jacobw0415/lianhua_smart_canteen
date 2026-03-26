package com.lianhua.erp.scheduler;

import com.lianhua.erp.config.AuditProperties;
import com.lianhua.erp.service.impl.ActivityAuditRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 活動稽核保留期：歸檔後刪除。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityAuditRetentionScheduler {

    private final ActivityAuditRetentionService retentionService;
    private final AuditProperties auditProperties;

    @Scheduled(cron = "${app.audit.retention.cron:0 0 2 * * *}")
    public void run() {
        AuditProperties.Retention cfg = auditProperties.getRetention();
        if (cfg == null || !cfg.isEnabled()) {
            return;
        }
        retentionService.archiveAndDeleteExpired();
    }
}

