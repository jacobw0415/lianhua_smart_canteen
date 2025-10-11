package com.lianhua.erp.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "付款紀錄 DTO（在建立時不需提供 id 與 createdAt）")
public class PaymentDto {
    
    @Schema(description = "付款紀錄 ID（系統自動生成）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    
    @Schema(description = "付款金額", example = "500.00", required = true)
    private BigDecimal amount;
    
    @Schema(description = "付款日期", example = "2025-10-11", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate payDate;
    
    @Schema(description = "付款方式（CASH / TRANSFER / CARD / CHECK）", example = "CASH", required = true)
    private String method;
    
    @Schema(description = "備註", example = "部分付款")
    private String note;
    
    @Schema(description = "建立時間（系統自動生成）", example = "2025-10-11T06:45:17.694Z", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;
}
