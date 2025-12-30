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
@Schema(description = "進貨單回應 DTO（含付款資料與自動計算）")
public class PurchaseDto {
    
    @Schema(description = "進貨單 ID", example = "1")
    private Long id;
    
    @Schema(description = "供應商 ID", example = "4")
    private Long supplierId;
    
    @Schema(description = "供應商名稱", example = "大福蔬菜有限公司")
    private String supplierName;
    
    @Schema(description = "進貨日期", example = "2025-10-11")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;
    
    @Schema(description = "採購明細列表")
    private List<PurchaseItemDto> items;
    
    @Schema(description = "狀態", example = "PARTIAL")
    private String status;
    
    @Schema(description = "付款總額（由明細計算）", example = "1600")
    private BigDecimal totalAmount;
    
    @Schema(description = "已付款金額", example = "500")
    private BigDecimal paidAmount;
    
    @Schema(description = "未付款金額", example = "1100")
    private BigDecimal balance;
    
    @Schema(description = "付款紀錄清單")
    private List<PaymentDto> payments;
}
