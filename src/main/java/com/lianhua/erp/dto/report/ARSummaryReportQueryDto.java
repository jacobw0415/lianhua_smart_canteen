package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 📌 應收帳款總表查詢條件
 *
 * 與資產負債表一致，支援：
 * - 單一月份 period=YYYY-MM
 * - 多月份 periods=YYYY-MM,YYYY-MM
 * - 截止日期 endDate=yyyy-MM-dd
 */
@Data
@Schema(description = "應收帳款總表查詢條件")
public class ARSummaryReportQueryDto {

    @Schema(description = "會計期間（YYYY-MM），優先使用 periods", example = "2025-10")
    private String period;

    @Schema(description = "多個會計期間（YYYY-MM），逗號分隔或陣列格式", example = "2025-10,2025-11,2025-12")
    private List<String> periods;

    @Schema(description = "截止日期（yyyy-MM-dd），若未提供 period/periods 則使用", example = "2025-12-31")
    private String endDate;

    public boolean hasValidQuery() {
        return (periods != null && !periods.isEmpty())
                || (period != null && !period.isBlank())
                || (endDate != null && !endDate.isBlank());
    }

    public List<String> getPeriodsList() {
        if (periods != null && !periods.isEmpty()) {
            return periods;
        }
        if (period != null && !period.isBlank()) {
            return List.of(period);
        }
        return List.of();
    }
}
