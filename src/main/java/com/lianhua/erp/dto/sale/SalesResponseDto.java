package com.lianhua.erp.dto.sale;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "銷售資料回應 DTO，包含商品與付款相關資訊")
public class SalesResponseDto {
    
    @Schema(description = "銷售紀錄 ID", example = "101")
    private Long id;
    
    @Schema(description = "銷售日期（格式：YYYY-MM-DD）", example = "2025-10-16")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate saleDate;
    
    @Schema(description = "商品 ID", example = "3")
    private Long productId;
    
    @Schema(description = "商品名稱", example = "香菇素便當")
    private String productName;
    
    @Schema(description = "銷售數量", example = "15")
    private Integer qty;
    
    @Schema(description = "總金額（未含稅）", example = "750.00")
    private BigDecimal amount;
    
    @Schema(description = "付款方式（CASH、CARD、MOBILE）", example = "CASH")
    private String payMethod;
    
    @Schema(description = "建立時間（系統自動生成）", example = "2025-10-16T11:25:45")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime createdAt;
    
    @Schema(description = "最後更新時間（系統自動更新）", example = "2025-10-16T11:30:10")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime updatedAt;
}
