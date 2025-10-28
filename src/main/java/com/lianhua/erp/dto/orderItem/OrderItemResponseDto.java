package com.lianhua.erp.dto.orderItem;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "訂單明細回應 DTO")
public class OrderItemResponseDto {
    
    @Schema(description = "明細 ID", example = "2001")
    private Long id;
    
    @Schema(description = "商品 ID", example = "5001")
    private Long productId;
    
    @Schema(description = "商品名稱", example = "蘋果汁 1L")
    private String productName;
    
    @Schema(description = "商品數量", example = "50")
    private Integer qty;
    
    @Schema(description = "單價", example = "80.5")
    private BigDecimal unitPrice;
    
    @Schema(description = "折扣金額", example = "50.0")
    private BigDecimal discount;
    
    @Schema(description = "稅額", example = "202.5")
    private BigDecimal tax;
    
    @Schema(
            description = "小計金額（系統自動計算：quantity × unitPrice - discount + tax）",
            example = "4050.0",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private BigDecimal subtotal;
    
    @Schema(description = "備註", example = "此商品需冷藏保存")
    private String note;
    
    @Schema(description = "建立時間", example = "2025-10-25T09:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新時間", example = "2025-10-27T14:45:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime updatedAt;
}
