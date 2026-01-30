package com.lianhua.erp.dto.dashboard.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "供應商採購集中度分析")
public record SupplierConcentrationDto(
        @Schema(description = "供應商名稱", example = "大盤農產公司")
        String supplierName,

        @Schema(description = "採購金額佔比 (%)", example = "35.2")
        double ratio,

        @Schema(description = "採購總金額", example = "120000.00")
        BigDecimal totalAmount
) {}