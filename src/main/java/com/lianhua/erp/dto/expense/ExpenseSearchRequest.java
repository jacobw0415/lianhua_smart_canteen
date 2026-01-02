package com.lianhua.erp.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "費用支出搜尋條件")
public class ExpenseSearchRequest {

    @Schema(
            description = "費用類別名稱（模糊搜尋）",
            example = "食材費"
    )
    private String categoryName;

    @Schema(
            description = "費用類別 ID（精準搜尋）",
            example = "3"
    )
    private Long categoryId;

    @Schema(
            description = "員工名稱（模糊搜尋）",
            example = "王小明"
    )
    private String employeeName;

    @Schema(
            description = "員工 ID（精準搜尋）",
            example = "5"
    )
    private Long employeeId;

    @Schema(
            description = "會計期間（精準搜尋）格式：YYYY-MM",
            example = "2025-01"
    )
    private String accountingPeriod;

    @Schema(
            description = "支出日期（起）格式：YYYY-MM-DD\n搜尋 expenseDate >= fromDate",
            example = "2025-01-01"
    )
    private String fromDate;

    @Schema(
            description = "支出日期（迄）格式：YYYY-MM-DD\n搜尋 expenseDate <= toDate",
            example = "2025-01-31"
    )
    private String toDate;

    @Schema(
            description = "備註（模糊搜尋）",
            example = "進貨"
    )
    private String note;

    @Schema(description = "是否包含已作廢的支出（預設 false，向後相容）", example = "false")
    private Boolean includeVoided = false;

    @Schema(description = "狀態過濾：支援精確匹配（ACTIVE/VOIDED）或模糊搜尋（作廢/有效/正常等關鍵詞）", 
            example = "作廢", allowableValues = {"ACTIVE", "VOIDED", "作廢", "有效", "正常"})
    private String status;
}

