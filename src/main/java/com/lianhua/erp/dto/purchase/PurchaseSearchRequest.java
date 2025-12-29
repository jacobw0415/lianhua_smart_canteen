package com.lianhua.erp.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "進貨單查詢條件")
public class PurchaseSearchRequest {

    @Schema(
            description = "供應商名稱（模糊搜尋）\n例如：'興美'、'清潔用品'",
            example = "興美"
    )
    private String supplierName;

    @Schema(
            description = "品項名稱（模糊搜尋）\n例如：'雞蛋'、'豆包'",
            example = "雞蛋"
    )
    private String item;

    @Schema(
            description = "狀態（精準搜尋）\nUNPAID: 未付款, PARTIAL: 部分付款, PAID: 已付款",
            example = "PAID"
    )
    private String status;

    @Schema(
            description = "會計期間（精準搜尋）格式：YYYY-MM\n例如：'2025-01'",
            example = "2025-01"
    )
    private String accountingPeriod;

    @Schema(
            description = "供應商編號（精準搜尋）\n對應 suppliers.id",
            example = "3"
    )
    private Long supplierId;

    @Schema(
            description = "進貨單編號（模糊搜尋）\n例如：'PO-202501-0001'",
            example = "PO-202501"
    )
    private String purchaseNo;

    @Schema(
            description = "進貨日期（起）格式：YYYY-MM-DD\n搜尋 purchaseDate >= fromDate",
            example = "2025-01-01"
    )
    private String fromDate;

    @Schema(
            description = "進貨日期（迄）格式：YYYY-MM-DD\n搜尋 purchaseDate <= toDate",
            example = "2025-01-31"
    )
    private String toDate;
}
