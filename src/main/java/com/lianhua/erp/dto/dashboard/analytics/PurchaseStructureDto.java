package com.lianhua.erp.dto.dashboard.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "採購結構分析數據 (依進貨項目)")
public record PurchaseStructureDto(
        @Schema(description = "進貨項目名稱 (item)", example = "特選高山烏龍")
        String itemName,

        @Schema(description = "採購總金額 (不含作廢單)", example = "150000.00")
        BigDecimal totalAmount
) {}