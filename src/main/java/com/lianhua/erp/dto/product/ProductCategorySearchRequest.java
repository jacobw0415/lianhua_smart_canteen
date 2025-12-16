package com.lianhua.erp.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "商品分類搜尋條件")
public class ProductCategorySearchRequest {
    @Schema(description = "分類名稱", example = "素食便當")
    private String name;

    @Schema(description = "分類代碼", example = "VEG")
    private String code;

    @Schema(description = "是否啟用", example = "true")
    private Boolean active;
}
