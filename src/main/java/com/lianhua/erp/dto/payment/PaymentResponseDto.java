package com.lianhua.erp.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "付款紀錄回應資料")
public class PaymentResponseDto {
    
    @Schema(description = "付款紀錄 ID", example = "50")
    private Long id;
    
    @Schema(description = "對應的進貨單 ID", example = "10")
    private Long purchaseId;
    
    @Schema(description = "付款金額", example = "1200.00")
    private BigDecimal amount;
    
    @Schema(description = "付款日期", example = "2025-10-12")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate payDate;
    
    @Schema(description = "付款方式", example = "TRANSFER")
    private String method;
    
    @Schema(description = "付款參考號碼", example = "TXN-20251012-001")
    private String referenceNo;
    
    @Schema(description = "付款備註", example = "已付款一半")
    private String note;
}
