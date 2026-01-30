package com.lianhua.erp.dto.dashboard.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "損益平衡分析數據點")
public record BreakEvenPointDto(
        @Schema(description = "日期", example = "2026-01-15")
        java.time.LocalDate date,

        @Schema(description = "當日累計營收 (Running Total)", example = "125000.00")
        BigDecimal runningRevenue,

        @Schema(description = "當日累計支出 (Running Total)", example = "98000.00")
        BigDecimal runningExpense,

        @Schema(description = "損益平衡門檻 (當月總固定支出)", example = "100000.00")
        BigDecimal breakEvenThreshold
) {}
