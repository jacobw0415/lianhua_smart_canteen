package com.lianhua.erp.dto.receipt;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "收款回應 DTO")
public class ReceiptResponseDto {
    
    @Schema(description = "收款記錄 ID", example = "1")
    private Long id;
    
    @Schema(description = "訂單 ID", example = "1")
    private Long orderId;
    
    @Schema(description = "收款日期", example = "2025-10-30")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate receivedDate;
    
    @Schema(description = "會計期間", example = "2025-10")
    private String accountingPeriod;
    
    @Schema(description = "收款金額（自動帶入）", example = "1200.00")
    private BigDecimal amount;
    
    @Schema(description = "收款方式", example = "TRANSFER")
    private String method;
    
    @Schema(description = "參考號碼", example = "TRX-20251030-001")
    private String referenceNo;
    
    @Schema(description = "備註", example = "自動對帳入帳")
    private String note;
    
    
    @Schema(description = "建立時間")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新時間")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime updatedAt;
}
