package com.lianhua.erp.dto.purchase;

import com.lianhua.erp.dto.payment.PaymentRequestDto;
import com.lianhua.erp.dto.validation.BaseRequestDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "進貨單建立或修改請求資料")
public class PurchaseRequestDto extends BaseRequestDto {
    
    @Schema(description = "供應商 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long supplierId;
    
    @Schema(description = "品名", example = "有機豆腐")
    private String item;
    
    @Schema(description = "數量", example = "100")
    private Integer qty;
    
    @Schema(description = "單價", example = "25.50")
    private BigDecimal unitPrice;
    
    @Schema(description = "進貨日期", example = "2025-10-12")
    private LocalDate purchaseDate;
    
    @Schema(description = "備註", example = "10月第一批原料")
    private String note;
    
    @Schema(description = "付款明細列表")
    private List<PaymentRequestDto> payments;
    
    @Schema(description = "付款狀態", example = "PENDING", allowableValues = {"PENDING", "PARTIAL", "PAID"})
    private String status;
}