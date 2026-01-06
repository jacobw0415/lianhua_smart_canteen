package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 💰 現金流量表查詢條件 DTO
 * 
 * 用於前端時間選擇器傳遞查詢參數
 */
@Data
@Schema(description = "現金流量表查詢條件")
public class CashFlowReportQueryDto {

    @Schema(
        description = "會計期間（YYYY-MM）\n" +
                     "優先使用此參數，如果提供則忽略 startDate 和 endDate",
        example = "2025-10"
    )
    private String period;

    @Schema(
        description = "起始日期（yyyy-MM-dd）\n" +
                     "與 endDate 一起使用，用於查詢日期區間",
        example = "2025-01-01"
    )
    private String startDate;

    @Schema(
        description = "結束日期（yyyy-MM-dd）\n" +
                     "與 startDate 一起使用，用於查詢日期區間",
        example = "2025-12-31"
    )
    private String endDate;

    /**
     * 驗證查詢參數是否有效
     * @return true 如果至少有一個有效的查詢條件
     */
    public boolean hasValidQuery() {
        return (period != null && !period.isBlank()) ||
               (startDate != null && endDate != null && 
                !startDate.isBlank() && !endDate.isBlank());
    }
}

