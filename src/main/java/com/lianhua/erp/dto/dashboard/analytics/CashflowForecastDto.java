package com.lianhua.erp.dto.dashboard.analytics;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "未來現金流預測數據點")
public record CashflowForecastDto(
        @Schema(description = "預測日期", example = "2026-02-10")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Taipei")
        java.time.LocalDate date,

        @Schema(description = "預計流入金額", example = "15000.00")
        BigDecimal inflow,

        @Schema(description = "預計流出金額", example = "8000.00")
        BigDecimal outflow
) {}