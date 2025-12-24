package com.lianhua.erp.dto.receipt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "收款紀錄搜尋條件")
public class ReceiptSearchRequest {

    @Schema(description = "收款 ID（精確查詢）", example = "1")
    private Long id;

    @Schema(description = "客戶名稱（模糊搜尋）", example = "科技公司")
    private String customerName;

    @Schema(description = "訂單編號（模糊搜尋）", example = "SO-202412")
    private String orderNo;

    @Schema(description = "收款方式（精準查詢）", example = "TRANSFER", allowableValues = { "CASH", "TRANSFER", "CARD",
                    "CHECK" })
    private String method;

    @Schema(description = "會計期間（YYYY-MM，精準查詢）", example = "2025-12")
    private String accountingPeriod;

    @Schema(description = "收款日期（起始，格式 YYYY-MM-DD）", example = "2025-01-01")
    private String fromDate;

    @Schema(description = "收款日期（結束，格式 YYYY-MM-DD）", example = "2025-01-31")
    private String toDate;

    @Schema(description = "收款日期（起始，LocalDate 格式）", example = "2025-01-01")
    private LocalDate receivedDateFrom;

    @Schema(description = "收款日期（結束，LocalDate 格式）", example = "2025-01-31")
    private LocalDate receivedDateTo;

    @Schema(description = "是否包含已作廢的收款（預設 false，向後相容）", example = "false")
    private Boolean includeVoided = false;

    @Schema(description = "狀態過濾：ACTIVE（只搜尋有效的）, VOIDED（只搜尋作廢的）, 或不指定（搜尋全部）", 
            example = "ACTIVE", allowableValues = {"ACTIVE", "VOIDED"})
    private String status;
}
