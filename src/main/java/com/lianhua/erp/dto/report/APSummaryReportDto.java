package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 📊 應付帳款總表 DTO（按期間彙總）
 * <p>
 * 對應資料庫來源：purchases (進貨單)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "應付帳款總表（按會計期間彙總）")
public class APSummaryReportDto {

    @Schema(description = "會計期間 (YYYY-MM 或 yyyy-MM-dd)", example = "2026-01")
    private String accountingPeriod;

    @Schema(description = "應付總額 (所有有效進貨單)", example = "50000.00")
    private BigDecimal totalPayable;

    @Schema(description = "已付金額 (已支付給廠商)", example = "20000.00")
    private BigDecimal totalPaid;

    @Schema(description = "未付金額（剩餘欠款/負債）", example = "30000.00")
    private BigDecimal totalOutstanding;
}
