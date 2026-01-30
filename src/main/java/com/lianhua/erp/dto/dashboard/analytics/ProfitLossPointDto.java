package com.lianhua.erp.dto.dashboard.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "損益趨勢數據點")
public record ProfitLossPointDto(
        @Schema(description = "統計期間 (月份或日期)", example = "2026-01")
        String period,

        @Schema(description = "總營收", example = "150000.00")
        BigDecimal revenue,

        @Schema(description = "銷售毛利", example = "85000.00")
        BigDecimal grossProfit,

        @Schema(description = "費用支出", example = "35000.00")
        BigDecimal expense,

        @Schema(description = "稅前淨利", example = "50000.00")
        BigDecimal netProfit
) {}