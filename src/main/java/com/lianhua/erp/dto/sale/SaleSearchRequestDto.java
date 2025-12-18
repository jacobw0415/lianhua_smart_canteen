package com.lianhua.erp.dto.sale;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "銷售查詢條件（支援模糊搜尋與分頁）")
public class SaleSearchRequestDto {

    /* =========================
     * 🔍 模糊 / 條件搜尋
     * ========================= */

    @Schema(description = "商品名稱（模糊搜尋）")
    private String productName;

    @Schema(description = "付款方式（CASH / TRANSFER / CARD / CHECK）")
    private String payMethod;

    @Schema(description = "銷售日期（起）")
    private LocalDate saleDateFrom;

    @Schema(description = "銷售日期（迄）")
    private LocalDate saleDateTo;

    /* =========================
     * 📄 分頁設定
     * ========================= */

    @Schema(description = "頁碼（0-based）", example = "0")
    private Integer page = 0;

    @Schema(description = "每頁筆數", example = "10")
    private Integer size = 10;

    @Schema(description = "排序欄位", example = "saleDate")
    private String sortBy = "saleDate";

    @Schema(description = "排序方向（ASC / DESC）", example = "DESC")
    private String sortDirection = "DESC";
}
