package com.lianhua.erp.dto.dashboard.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "訂單履約階段分析")
public record OrderFunnelDto(
        @Schema(description = "流程階段", example = "已出貨", allowableValues = {"草稿", "已確認", "已出貨", "已結案"})
        String stage,

        @Schema(description = "該階段訂單筆數", example = "12")
        Integer orderCount,

        @Schema(description = "該階段涉及總金額", example = "89000.00")
        BigDecimal totalAmount
) {}