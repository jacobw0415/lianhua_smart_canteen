package com.lianhua.erp.dto.audit;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * 活動稽核分頁查詢／匯出共用篩選條件（與其他模組 SearchRequest 模式一致）。
 */
@Schema(description = "活動稽核篩選條件")
public record ActivityAuditLogSearchRequest(
        @Schema(description = "關鍵字（模糊搜尋）", example = "employees", nullable = true)
        String keyword,

        @Schema(description = "操作者使用者名稱（模糊搜尋）", example = "admin", nullable = true)
        String operatorUsername,

        @Schema(description = "操作者使用者 ID") Long operatorId,
        @Schema(description = "動作（CREATE、UPDATE、DELETE、EXPORT 等）") String action,
        @Schema(description = "資源類型（不分大小寫）") String resourceType,
        @Schema(description = "發生時間起（含）")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @Schema(description = "發生時間迄（含）")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
) {
}
