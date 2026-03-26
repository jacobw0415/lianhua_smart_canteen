package com.lianhua.erp.service.impl;

import com.lianhua.erp.config.AuditProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * 活動稽核保留期維護：
 * 1) 將過期資料批次歸檔到 activity_audit_logs_archive
 * 2) 成功搬移後再刪除原始 activity_audit_logs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityAuditRetentionService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditProperties auditProperties;
    private final TransactionTemplate transactionTemplate;

    private static final String INSERT_ARCHIVE_SQL = """
            INSERT INTO activity_audit_logs_archive
              (id, occurred_at, operator_id, operator_username, action, resource_type, resource_id,
               http_method, request_path, query_string, ip_address, user_agent, details)
            SELECT
              a.id, a.occurred_at, a.operator_id, a.operator_username, a.action, a.resource_type, a.resource_id,
              a.http_method, a.request_path, a.query_string, a.ip_address, a.user_agent, a.details
            FROM activity_audit_logs a
            WHERE a.id IN (
              SELECT id FROM (
                SELECT id
                FROM activity_audit_logs
                WHERE occurred_at < ?
                ORDER BY occurred_at ASC, id ASC
                LIMIT ?
              ) t
            )
            """;

    private static final String DELETE_MAIN_SQL = """
            DELETE FROM activity_audit_logs
            WHERE id IN (
              SELECT id FROM (
                SELECT id
                FROM activity_audit_logs
                WHERE occurred_at < ?
                ORDER BY occurred_at ASC, id ASC
                LIMIT ?
              ) t
            )
            """;

    public int archiveAndDeleteExpired() {
        AuditProperties.Retention cfg = auditProperties.getRetention();
        if (cfg == null || !cfg.isEnabled()) {
            return 0;
        }

        int days = Math.max(cfg.getDays(), 0);
        if (days == 0) {
            return 0;
        }

        int batchSize = Math.max(cfg.getBatchSize(), 1);

        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        Timestamp cutoffTs = Timestamp.from(cutoff);

        int totalArchived = 0;
        while (true) {
            int inserted = archiveAndDeleteBatch(cutoffTs, batchSize);
            if (inserted <= 0) {
                break;
            }
            totalArchived += inserted;

            // 若小於 batchSize，代表後面大概率沒有更多符合條件的資料，直接跳出
            if (inserted < batchSize) {
                break;
            }
        }

        if (totalArchived > 0) {
            log.info("Activity audit retention finished: archived {} rows older than {} days", totalArchived, days);
        }
        return totalArchived;
    }

    private int archiveAndDeleteBatch(Timestamp cutoffTs, int batchSize) {
        // 注意：不要依賴 self-invocation 的 @Transactional（Spring AOP 不會拦截同 class 呼叫）
        // 這裡用 TransactionTemplate 明確保證 insert + delete 同一個 transaction。
        return transactionTemplate.execute(status -> {
            int inserted = jdbcTemplate.update(INSERT_ARCHIVE_SQL, cutoffTs, batchSize);
            if (inserted <= 0) {
                return 0;
            }
            jdbcTemplate.update(DELETE_MAIN_SQL, cutoffTs, batchSize);
            return inserted;
        });
    }
}

