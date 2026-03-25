package com.lianhua.erp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 全系統活動稽核紀錄（由 HTTP 攔截器寫入；不含登入前匿名請求）。
 */
@Entity
@Table(name = "activity_audit_logs", indexes = {
        @Index(name = "idx_activity_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_activity_operator", columnList = "operator_id"),
        @Index(name = "idx_activity_resource", columnList = "resource_type,resource_id"),
        @Index(name = "idx_activity_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "TIMESTAMP(3)")
    private Instant occurredAt;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    /** 操作者使用者名稱快照（不隨帳號改名而變更） */
    @Column(name = "operator_username", length = 60)
    private String operatorUsername;

    /** CREATE, UPDATE, DELETE, EXPORT, PATCH */
    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "request_path", nullable = false, length = 1024)
    private String requestPath;

    @Column(name = "query_string", length = 512)
    private String queryString;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String details;
}
