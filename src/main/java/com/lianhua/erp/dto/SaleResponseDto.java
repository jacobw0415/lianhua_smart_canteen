package com.lianhua.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "銷售回應 DTO")
public class SaleResponseDto {

    @Schema(description = "銷售 ID", example = "101")
    private Long id;

    @Schema(description = "日期", example = "2025-10-03")
    private String saleDate;

    @Schema(description = "數量", example = "2")
    private Integer qty;

    @Schema(description = "金額", example = "180.00")
    private Double amount;

    @Schema(description = "付款方式", example = "CASH")
    private String payMethod;

    @Schema(description = "產品資訊")
    private ProductDto product; // 🔑 直接嵌入完整產品
}
