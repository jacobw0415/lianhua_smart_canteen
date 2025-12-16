package com.lianhua.erp.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "商品查詢條件（前端使用者）")
public class ProductSearchRequest {

    @Schema(description = "商品名稱（模糊搜尋）", example = "便當")
    private String name;

    @Schema(description = "商品代碼（模糊搜尋）", example = "P001")
    private String code;

    @Schema(description = "是否啟用", example = "true")
    private Boolean active;

    @Schema(description = "分類 ID", example = "1")
    private Long categoryId;

    @Schema(description = "最低單價", example = "50.00")
    private BigDecimal unitPriceMin;

    @Schema(description = "最高單價", example = "120.00")
    private BigDecimal unitPriceMax;
}
