package com.lianhua.erp.dto.audit;

/**
 * 寫入 activity_audit_logs 的請求（由攔截器組裝）。
 */
public record ActivityAuditRecordRequest(
        Long operatorId,
        String operatorUsername,
        String action,
        String resourceType,
        Long resourceId,
        String httpMethod,
        String requestPath,
        String queryString,
        String ipAddress,
        String userAgent,
        String details
) {
}
