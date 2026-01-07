package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 💼 綜合損益表 DTO（Comprehensive Income Statement）
 * 
 * 📌 會計定義：
 * - 綜合損益表為「期間報表」，顯示特定期間的收入、成本、費用與淨利
 * - 包含營業收入、營業成本、營業費用、其他收入/支出等明細
 * 
 * 📌 計算結構：
 * 1. 營業收入 = 零售銷售 + 訂單銷售
 * 2. 營業成本 = 採購成本
 * 3. 毛利益 = 營業收入 - 營業成本
 * 4. 營業費用 = 各項費用總和（按類別分類）
 * 5. 營業利益 = 毛利益 - 營業費用
 * 6. 其他收入 = （預留欄位）
 * 7. 其他支出 = （預留欄位）
 * 8. 本期淨利 = 營業利益 + 其他收入 - 其他支出
 * 9. 其他綜合損益 = （預留欄位，如匯率影響等）
 * 10. 綜合損益總額 = 本期淨利 + 其他綜合損益
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "綜合損益表回應 DTO（含詳細收入、成本、費用明細）")
public class ComprehensiveIncomeStatementDto {

    @Schema(description = "會計期間（YYYY-MM）", example = "2025-10")
    private String accountingPeriod;

    // ========== 營業收入 ==========
    @Schema(description = "零售銷售收入（Sales 表）", example = "45800.00")
    private BigDecimal retailSales;

    @Schema(description = "訂單銷售收入（Orders 表）", example = "78200.00")
    private BigDecimal orderSales;

    @Schema(description = "營業收入合計（retailSales + orderSales）", example = "124000.00")
    private BigDecimal totalRevenue;

    // ========== 營業成本 ==========
    @Schema(description = "採購成本（Purchases 表）", example = "73500.00")
    private BigDecimal costOfGoodsSold;

    // ========== 毛利益 ==========
    @Schema(description = "毛利益（totalRevenue - costOfGoodsSold）", example = "50500.00")
    private BigDecimal grossProfit;

    // ========== 營業費用（按類別明細）==========
    @Schema(description = "費用類別明細列表（包含類別名稱、金額）")
    private List<ExpenseCategoryDetailDto> expenseDetails;

    @Schema(description = "營業費用合計", example = "18400.00")
    private BigDecimal totalOperatingExpenses;

    // ========== 營業利益 ==========
    @Schema(description = "營業利益（grossProfit - totalOperatingExpenses）", example = "32100.00")
    private BigDecimal operatingProfit;

    // ========== 其他收入/支出 ==========
    @Schema(description = "其他收入（預留欄位，未來可擴充）", example = "0.00")
    private BigDecimal otherIncome;

    @Schema(description = "其他支出（預留欄位，未來可擴充）", example = "0.00")
    private BigDecimal otherExpenses;

    // ========== 本期淨利 ==========
    @Schema(description = "本期淨利（operatingProfit + otherIncome - otherExpenses）", example = "32100.00")
    private BigDecimal netProfit;

    // ========== 其他綜合損益 ==========
    @Schema(description = "其他綜合損益（預留欄位，如匯率影響、重估增值等）", example = "0.00")
    private BigDecimal otherComprehensiveIncome;

    // ========== 綜合損益總額 ==========
    @Schema(description = "綜合損益總額（netProfit + otherComprehensiveIncome）", example = "32100.00")
    private BigDecimal comprehensiveIncome;

    /**
     * 費用類別明細 DTO（內部使用）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "費用類別明細")
    public static class ExpenseCategoryDetailDto {
        @Schema(description = "費用類別 ID")
        private Long categoryId;

        @Schema(description = "費用類別名稱", example = "食材費")
        private String categoryName;

        @Schema(description = "費用類別會計代碼", example = "EXP-001")
        private String accountCode;

        @Schema(description = "該類別費用總額", example = "8500.00")
        private BigDecimal amount;

        @Schema(description = "是否為薪資類別", example = "false")
        private Boolean isSalary;
    }
}

