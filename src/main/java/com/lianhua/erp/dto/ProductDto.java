package com.lianhua.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "商品 DTO")
public class ProductDto {
    @Schema(description = "商品 ID", example = "501")
    private Long id;

    @Schema(description = "名稱", example = "素食便當")
    private String name;

    @Schema(description = "分類", example = "VEG_LUNCHBOX")
    private String category;

    @Schema(description = "單價", example = "85.00")
    private Double unitPrice;

    @Schema(description = "是否啟用", example = "true")
    private Boolean active;
}
