package com.lianhua.erp.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "時間序列趨勢數據點")
public record TrendPointDto(
        @Schema(description = "統計日期", example = "2026-01-27")
        LocalDate date,

        @Schema(description = "統計金額", example = "5200.00")
        BigDecimal amount,

        @Schema(description = "數據類型 (SALES: 銷售, EXPENSE: 支出, PURCHASE: 採購)", example = "SALES")
        String type
) {
}