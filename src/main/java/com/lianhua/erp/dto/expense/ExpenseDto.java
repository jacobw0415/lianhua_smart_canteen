package com.lianhua.erp.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "開支 DTO")
public class ExpenseDto {
    @Schema(description = "開支 ID", example = "101")
    private Long id;

    @Schema(description = "開支日期", example = "2025-10-01")
    private String expenseDate;

    @Schema(description = "開支類別", example = "房租")
    private String category;

    @Schema(description = "金額", example = "35000.00")
    private Double amount;

    @Schema(description = "備註", example = "每月 5 號支付")
    private String note;

    @Schema(description = "員工 ID (若為薪資支出)", example = "5")
    private Long employeeId;
}

