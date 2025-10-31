package com.lianhua.erp.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "商品建立或更新請求資料")
public class ProductRequestDto {

    @Schema(description = "商品名稱", example = "香菇素便當")
    private String name;

    @Schema(description = "商品分類 ID", example = "1")
    private Long categoryId;

    @Schema(description = "單價", example = "75.00")
    private BigDecimal unitPrice;

    @Schema(description = "是否啟用", example = "true")
    private Boolean active = true;
}

