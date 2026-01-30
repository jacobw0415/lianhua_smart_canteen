package com.lianhua.erp.dto.dashboard.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "流動性與償債能力指標")
public record LiquidityDto(
        @Schema(description = "流動資產 (現金 + 應收)", example = "500000.00")
        BigDecimal liquidAssets,

        @Schema(description = "流動負債 (應付帳款)", example = "200000.00")
        BigDecimal liquidLiabilities,

        @Schema(description = "速動資產 (即時可用現金)", example = "350000.00")
        BigDecimal quickAssets
) {
    @Schema(description = "流動比率 (計算欄位)", example = "2.5")
    public double getCurrentRatio() {
        return liquidLiabilities.compareTo(BigDecimal.ZERO) > 0
                ? liquidAssets.divide(liquidLiabilities, 2, java.math.RoundingMode.HALF_UP).doubleValue()
                : 0;
    }
}
