package com.lianhua.erp.dto.ar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "📌 客戶逐筆訂單應收帳款明細（AR Detail）")
public class ARAgingOrderDetailDto {

    @Schema(description = "React-Admin 用 id（= orderId）")
    private Long id;

    @Schema(description = "訂單 ID", example = "105")
    private Long orderId;
    
    @Schema(description = "訂單編號（商業單號）", example = "ORD-202501-0008")
    private String orderNo;

    @Schema(description = "訂單日期", example = "2025-01-08")
    private LocalDate orderDate;

    @Schema(description = "交貨日期", example = "2025-01-10")
    private LocalDate deliveryDate;

    @Schema(description = "該筆訂單金額（total amount）", example = "5000.00")
    private BigDecimal totalAmount;

    @Schema(description = "已收款金額", example = "3000.00")
    private BigDecimal receivedAmount;

    @Schema(description = "未收款金額（totalAmount - receivedAmount）", example = "2000.00")
    private BigDecimal balance;

    @Schema(description = "帳齡區間（以交貨日期為基準）", example = "31–60")
    private String agingBucket;

    @Schema(description = "逾期天數", example = "42")
    private Integer daysOverdue;
}

