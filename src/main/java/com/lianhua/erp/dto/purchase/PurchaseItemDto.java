package com.lianhua.erp.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "採購明細 DTO")
public class PurchaseItemDto {

  @Schema(description = "明細 ID", example = "1")
  private Long id;

  @Schema(description = "進貨項目", example = "高麗菜")
  private String item;

  @Schema(description = "數量單位（顯示用，例如：斤、箱、盒）", example = "斤")
  private String unit;

  @Schema(description = "數量", example = "100")
  private Integer qty;

  @Schema(description = "單價", example = "15.50")
  private BigDecimal unitPrice;

  @Schema(description = "小計（不含稅）", example = "1550.00")
  private BigDecimal subtotal;

  @Schema(description = "備註", example = "新鮮高麗菜")
  private String note;
}
