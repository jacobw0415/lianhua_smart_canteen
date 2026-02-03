package com.lianhua.erp.dto.dashboard.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "客戶採購集中度分析 DTO (Customer Concentration)")
public record CustomerConcentrationDto(
        @Schema(description = "客戶名稱", example = "蓮花大飯店")
        String customerName,

        @Schema(description = "營收佔比 (%)", example = "25.5")
        double ratio,

        @Schema(description = "期間內累計採購總額", example = "500000.00")
        BigDecimal totalAmount
) {}