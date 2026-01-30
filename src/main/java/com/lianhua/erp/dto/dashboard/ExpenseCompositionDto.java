package com.lianhua.erp.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "儀表板：支出結構比例數據")
public class ExpenseCompositionDto {

    @Schema(description = "支出類別名稱", example = "進貨採購")
    private String category;

    @Schema(description = "該類別支出總金額", example = "48000.00")
    private BigDecimal amount;
}