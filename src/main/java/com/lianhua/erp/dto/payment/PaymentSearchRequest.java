package com.lianhua.erp.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "付款紀錄搜尋條件")
public class PaymentSearchRequest {

    @Schema(
            description = "供應商名稱（模糊搜尋）",
            example = "泰山蔬果供應行"
    )
    private String supplierName;

    @Schema(
            description = "品項摘要（模糊搜尋，用於找出付款所屬的進貨項目）",
            example = "高麗菜"
    )
    private String item;

    @Schema(
            description = "付款方式（精準查詢）",
            example = "TRANSFER",
            allowableValues = {"CASH", "TRANSFER", "CARD", "CHECK"}
    )
    private String method;

    @Schema(
            description = "會計期間（YYYY-MM，精準查詢）",
            example = "2025-12"
    )
    private String accountingPeriod;

    @Schema(
            description = "付款日期（起始，格式 YYYY-MM-DD）",
            example = "2025-01-01"
    )
    private String fromDate;

    @Schema(
            description = "付款日期（結束，格式 YYYY-MM-DD）",
            example = "2025-01-31"
    )
    private String toDate;

    @Schema(description = "是否包含已作廢的付款（預設 false）", example = "false")
    private Boolean includeVoided = false;
}
