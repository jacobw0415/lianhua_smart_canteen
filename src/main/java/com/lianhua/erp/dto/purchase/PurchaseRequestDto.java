package com.lianhua.erp.dto.purchase;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lianhua.erp.dto.payment.PaymentDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "建立或更新進貨單請求 DTO")
public class PurchaseRequestDto {
    
    @Schema(description = "供應商 ID", example = "4")
    private Long supplierId;
    
    @Schema(description = "進貨日期", example = "2025-10-11")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;
    
    @Schema(description = "進貨品項", example = "高麗菜")
    private String item;
    
    @Schema(description = "數量", example = "100")
    private Integer qty;
    
    @Schema(description = "單價", example = "15.5")
    private BigDecimal unitPrice;
    
    @Schema(description = "稅金", example = "50")
    private BigDecimal tax;
    
    @Schema(description = "狀態", example = "PENDING")
    private String status;
    
    @Schema(description = "付款紀錄清單（可選）")
    private List<PaymentDto> payments;
}
