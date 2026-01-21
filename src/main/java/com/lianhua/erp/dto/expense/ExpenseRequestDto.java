package com.lianhua.erp.dto.expense;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 用於新增或更新支出請求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "支出建立／更新請求 DTO")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExpenseRequestDto {

    @NotNull
    @Schema(description = "支出日期", example = "2025-10-15")
    private LocalDate expenseDate;

    @Schema(description = "費用類別 ID（選擇員工時可選，系統會自動選擇薪資類別）", example = "3")
    private Long categoryId;

    @Schema(description = "支出金額（新台幣，選擇員工時會自動填入員工薪資）", example = "1200.50")
    private BigDecimal amount;

    @Schema(description = "支出備註", example = "支付外送燃料費")
    private String note;

    @Schema(description = "對應員工 ID（若為薪資或個別費用）", example = "5")
    private Long employeeId;
}
