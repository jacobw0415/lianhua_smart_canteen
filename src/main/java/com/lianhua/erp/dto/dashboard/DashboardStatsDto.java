package com.lianhua.erp.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "儀表板核心統計指標摘要")
public record DashboardStatsDto(
        @Schema(description = "今日銷售總額", example = "4500.00")
        BigDecimal todaySalesTotal,

        @Schema(description = "本月銷售總額", example = "125000.00")
        BigDecimal monthSalesTotal,

        @Schema(description = "本月採購總額 (排除作廢)", example = "48000.00")
        BigDecimal monthPurchaseTotal,

        @Schema(description = "本月費用支出 (排除作廢)", example = "12000.00")
        BigDecimal monthExpenseTotal,

        @Schema(description = "活躍供應商總數", example = "12")
        long supplierCount,

        @Schema(description = "累計客戶總數", example = "150")
        long customerCount,

        @Schema(description = "目前上架商品總數", example = "45")
        long activeProductCount,

        @Schema(description = "待處理/確認中訂單數", example = "3")
        int pendingOrderCount,

        @Schema(description = "目前應付帳款總額 (AP)", example = "3200.00")
        BigDecimal accountsPayable,

        @Schema(description = "目前應收帳款總額 (AR)", example = "8500.00")
        BigDecimal accountsReceivable,

        @Schema(description = "本月預估淨利", example = "65000.00")
        BigDecimal netProfit,

        @Schema(description = "本月淨利率 (百分比)", example = "52.0")
        double profitMargin,
        BigDecimal todayReceiptsTotal, BigDecimal todayTotalInflow, BigDecimal monthTotalReceived,
        BigDecimal upcomingAR) {
}