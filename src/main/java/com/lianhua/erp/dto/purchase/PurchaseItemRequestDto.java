package com.lianhua.erp.dto.purchase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "採購明細請求 DTO")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PurchaseItemRequestDto {
    
    @Schema(description = "進貨項目", example = "高麗菜", requiredMode = Schema.RequiredMode.REQUIRED)
    private String item;
    
    @Schema(description = "數量單位（顯示用，例如：斤、箱、盒）", example = "斤", requiredMode = Schema.RequiredMode.REQUIRED)
    private String unit;
    
    @Schema(description = "數量", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer qty;
    
    @Schema(description = "單價", example = "15.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal unitPrice;

    @Schema(description = "備註", example = "新鮮高麗菜")
    private String note;
}

