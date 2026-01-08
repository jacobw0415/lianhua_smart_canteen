package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 📌 應付帳款總表查詢條件
 *
 * 與應收帳款 (AR) 一致，支援：
 * - 單一月份 period=YYYY-MM
 * - 多月份 periods=YYYY-MM,YYYY-MM
 * - 截止日期 endDate=yyyy-MM-dd
 */
@Data
@Schema(description = "應付帳款總表查詢條件")
public class APSummaryReportQueryDto {

    @Schema(description = "會計期間（YYYY-MM），優先使用 periods", example = "2026-01")
    private String period;

    @Schema(description = "多個會計期間（YYYY-MM），逗號分隔或陣列格式", example = "2026-01,2026-02,2026-03")
    private List<String> periods;

    @Schema(description = "截止日期（yyyy-MM-dd），若未提供 period/periods 則使用", example = "2026-01-31")
    private String endDate;

    /**
     * 檢查是否包含有效的查詢參數
     */
    public boolean hasValidQuery() {
        return (periods != null && !periods.isEmpty())
                || (period != null && !period.isBlank())
                || (endDate != null && !endDate.isBlank());
    }

    /**
     * 輔助方法：取得標準化的期間列表
     * 若 periods 為空但 period 有值，則自動包裝成 List
     */
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