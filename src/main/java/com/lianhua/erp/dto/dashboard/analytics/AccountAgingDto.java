package com.lianhua.erp.dto.dashboard.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "帳款帳齡風險分析")
public record AccountAgingDto(
        @Schema(description = "帳齡區段", example = "31-60天", allowableValues = {"0-30天", "31-60天", "61-90天", ">90天"})
        String bucketLabel,

        @Schema(description = "應收帳款 (AR) 金額", example = "45000.00")
        BigDecimal arAmount,

        @Schema(description = "應付帳款 (AP) 金額", example = "15000.00")
        BigDecimal apAmount
) {}