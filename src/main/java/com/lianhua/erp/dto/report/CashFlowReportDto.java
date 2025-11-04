package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * 💰 現金流量報表 DTO
 * 支援依月份或日期區間統計現金流入與流出。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "現金流量報表 DTO（支援月份與日期區間查詢）")
public class CashFlowReportDto {

    @Schema(description = "會計期間（YYYY-MM），或顯示 '合計 (yyyy-MM-dd ~ yyyy-MM-dd)'",
            example = "2025-10")
    private String accountingPeriod;

    @Schema(description = "零售現金收入 (Sales)", example = "30950.00")
    private BigDecimal totalSales;

    @Schema(description = "訂單收款收入 (Receipts)", example = "7200.00")
    private BigDecimal totalReceipts;

    @Schema(description = "採購付款支出 (Payments)", example = "5500.00")
    private BigDecimal totalPayments;

    @Schema(description = "營運費用支出 (Expenses)", example = "2200.00")
    private BigDecimal totalExpenses;

    @Schema(description = "總流入金額 (Sales + Receipts)", example = "38150.00")
    private BigDecimal totalInflow;

    @Schema(description = "總流出金額 (Payments + Expenses)", example = "7700.00")
    private BigDecimal totalOutflow;

    @Schema(description = "本期淨現金流 (Inflow - Outflow)", example = "30450.00")
    private BigDecimal netCashFlow;
}
