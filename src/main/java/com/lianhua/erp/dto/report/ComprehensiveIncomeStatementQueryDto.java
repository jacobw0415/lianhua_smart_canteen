package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 綜合損益表查詢條件 DTO
 */
@Data
@Schema(description = "綜合損益表查詢條件")
public class ComprehensiveIncomeStatementQueryDto {

    @Schema(description = "會計期間（YYYY-MM），例如：2025-10", example = "2025-10")
    private String period;

    @Schema(description = "起始日期（yyyy-MM-dd），與 endDate 一起使用", example = "2025-10-01")
    private String startDate;

    @Schema(description = "結束日期（yyyy-MM-dd），與 startDate 一起使用", example = "2025-10-31")
    private String endDate;

    @Schema(description = "多個會計期間列表（YYYY-MM），用於比較多個月份的損益", example = "[\"2025-10\", \"2025-11\", \"2025-12\"]")
    private List<String> periods;

    /**
     * 取得 periods 列表（處理 null 情況）
     */
    public List<String> getPeriodsList() {
        return periods != null ? periods : List.of();
    }
}

