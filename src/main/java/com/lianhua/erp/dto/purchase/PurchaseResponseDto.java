package com.lianhua.erp.dto.purchase;

import com.lianhua.erp.dto.payment.PaymentResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "進貨單回應資料（含付款明細）")
public class PurchaseResponseDto {
    
    @Schema(description = "進貨單 ID", example = "10")
    private Long id;
    
    @Schema(description = "供應商名稱", example = "蓮華素食供應商")
    private String supplierName;
    
    @Schema(description = "品名", example = "有機豆腐")
    private String item;
    
    @Schema(description = "數量", example = "100")
    private Integer qty;
    
    @Schema(description = "單價", example = "25.50")
    private BigDecimal unitPrice;
    
    @Schema(description = "稅額", example = "127.50")
    private BigDecimal taxAmount;
    
    @Schema(description = "總金額（含稅）", example = "2677.50")
    private BigDecimal totalAmount;
    
    @Schema(description = "付款狀態", example = "PARTIAL")
    private String status;
    
    @Schema(description = "進貨日期", example = "2025-10-12")
    private LocalDate purchaseDate;
    
    @Schema(description = "備註", example = "10月第一批原料")
    private String note;
    
    @Schema(description = "對應的付款明細列表")
    private List<PaymentResponseDto> payments;
}