package com.lianhua.erp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 使用者管理操作稽核日誌（§5 規格）
 * 記錄誰、何時、對哪個使用者、做了什麼操作；details 不得含密碼明文。
 */
@Entity
@Table(name = "user_audit_logs", indexes = {
        @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_operator", columnList = "operator_id"),
        @Index(name = "idx_audit_target", columnList = "target_user_id"),
        @Index(name = "idx_audit_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作發生時間（UTC） */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** 操作者使用者 id */
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    /** 被操作的使用者 id */
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    /** 操作類型：USER_CREATE, USER_UPDATE, USER_RESET_PASSWORD, USER_CHANGE_OWN_PASSWORD, USER_DELETE 等 */
    @Column(nullable = false, length = 50)
    private String action;

    /** 變更摘要（JSON 或文字），不得含密碼明文 */
    @Column(columnDefinition = "TEXT")
    private String details;
}
