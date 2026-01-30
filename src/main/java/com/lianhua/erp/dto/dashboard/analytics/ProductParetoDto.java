package com.lianhua.erp.dto.dashboard.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "商品獲利貢獻 Pareto 分析")
public record ProductParetoDto(
        @Schema(description = "商品名稱", example = "特選高山茶")
        String productName,

        @Schema(description = "銷售總額", example = "85000.00")
        BigDecimal totalAmount,

        @Schema(description = "累計貢獻百分比", example = "75.5")
        double cumulativePct
) {}
