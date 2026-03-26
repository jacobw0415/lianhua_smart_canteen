package com.lianhua.erp.dto.audit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "全系統活動稽核紀錄")
public record ActivityAuditLogResponseDto(
        @Schema(description = "紀錄 ID") Long id,
        @Schema(description = "發生時間 (UTC)") Instant occurredAt,
        @Schema(description = "發生時間（台北，+08:00）") String occurredAtTaipei,
        @Schema(description = "操作者使用者 ID") Long operatorId,
        @Schema(description = "操作者使用者名稱") String operatorUsername,
        @Schema(description = "動作") String action,
        @Schema(description = "資源類型（依 API 路徑推斷）") String resourceType,
        @Schema(description = "路徑中第一個數字 ID") Long resourceId,
        @Schema(description = "HTTP 方法") String httpMethod,
        @Schema(description = "回應狀態（HTTP status code）") Integer responseStatus,
        @Schema(description = "耗時（毫秒）") Long durationMs,
        @Schema(description = "請求路徑") String requestPath,
        @Schema(description = "查詢字串（截斷）") String queryString,
        @Schema(description = "來源 IP") String ipAddress,
        @Schema(description = "User-Agent（截斷）") String userAgent,
        @Schema(description = "補充資訊 JSON") String details
) {
}
