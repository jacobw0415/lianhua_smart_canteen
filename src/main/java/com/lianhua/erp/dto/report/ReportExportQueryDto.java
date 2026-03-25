package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 統一財務報表匯出查詢參數（與各報表 GET 可共用之欄位）。
 */
@Data
@Schema(description = "財務報表匯出查詢參數")
public class ReportExportQueryDto {

    @Schema(description = "會計期間 YYYY-MM")
    private String period;

    @Schema(description = "結束日期 yyyy-MM-dd")
    private String endDate;

    @Schema(description = "起始日期 yyyy-MM-dd（綜合損益、現金流）")
    private String startDate;

    @Schema(description = "多期間 YYYY-MM（陣列或逗號字串由 Controller 解析）")
    private List<String> periods;

    /**
     * 與各報表 QueryDto#getPeriodsList 相同語意。
     */
    public List<String> resolvedPeriodsList() {
        if (periods != null && !periods.isEmpty()) {
            return periods;
        }
        if (period != null && !period.isBlank()) {
            return List.of(period.trim());
        }
        return List.of();
    }
}
