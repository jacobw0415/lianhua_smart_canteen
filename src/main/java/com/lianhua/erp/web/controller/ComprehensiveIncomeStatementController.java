package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.report.ComprehensiveIncomeStatementDto;
import com.lianhua.erp.dto.report.ComprehensiveIncomeStatementQueryDto;
import com.lianhua.erp.service.ComprehensiveIncomeStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 💼 綜合損益表 Controller
 * 
 * API 路徑說明：
 * - GET /api/reports/comprehensive_income_statement
 * 
 * 前端使用：<Resource name="comprehensive_income_statement" list={ComprehensiveIncomeStatementList} />
 * 
 * 提供綜合損益表統計報表的 API 端點。
 * 綜合損益表是「期間報表」，顯示特定期間的收入、成本、費用與淨利。
 * 
 * 前端時間選擇器建議：
 * - 月份選擇器：傳遞 period 參數（YYYY-MM），查詢該月份的損益
 * - 日期選擇器：傳遞 startDate 和 endDate 參數（yyyy-MM-dd），查詢日期區間的損益
 * - 多期間比較：傳遞 periods 參數（多個 YYYY-MM），查詢多個月份的損益並列比較
 */
@RestController
@RequestMapping("/api/reports/comprehensive_income_statement")
@RequiredArgsConstructor
@Tag(name = "綜合損益表", description = "綜合損益表報表 API - 統計收入、成本、費用與淨利")
public class ComprehensiveIncomeStatementController {

    private final ComprehensiveIncomeStatementService comprehensiveIncomeStatementService;

    /**
     * 📊 生成綜合損益表（期間報表）
     * 
     * 綜合損益表顯示特定期間的收入、成本、費用與淨利（流量概念）。
     * 
     * 支援多種查詢方式：
     * 1. 多個月份：periods=2025-10,2025-11,2025-12（查詢多個月份並列比較）
     * 2. 指定月份：period=2025-10（查詢2025-10月份的損益）
     * 3. 指定日期區間：startDate=2025-10-01&endDate=2025-10-31（查詢日期區間的損益）
     * 
     * @param query 查詢條件（包含 periods、period 或 startDate/endDate）
     * @return 綜合損益表報表資料列表（包含各期間明細及合計）
     */
    @GetMapping
    @Operation(summary = "查詢綜合損益表（期間報表）", description = """
        查詢綜合損益表，顯示特定期間的收入、成本、費用與淨利。

        支援以下查詢方式：
        1. 多個月份：periods=2025-10,2025-11,2025-12（查詢多個月份並列比較）⭐ 新增
        2. 指定月份：period=2025-10（查詢2025-10月份的損益）
        3. 指定日期區間：startDate=2025-10-01&endDate=2025-10-31（查詢日期區間的損益）

        查詢優先級：
        - 如果提供 periods，優先使用（忽略 period、startDate 和 endDate）
        - 如果提供 period，使用單一月份查詢
        - 如果提供 startDate 和 endDate，使用日期區間查詢

        報表包含：
        - 營業收入：零售銷售收入 + 訂單銷售收入
        - 營業成本：採購成本
        - 毛利益：營業收入 - 營業成本
        - 營業費用：按費用類別分類的明細（包含類別名稱、會計代碼、金額）
        - 營業利益：毛利益 - 營業費用
        - 其他收入/支出：預留欄位（目前為 0）
        - 本期淨利：營業利益 + 其他收入 - 其他支出
        - 其他綜合損益：預留欄位（目前為 0）
        - 綜合損益總額：本期淨利 + 其他綜合損益

        💡 前端時間選擇器建議：
        - 使用 @ParameterObject 自動綁定查詢參數
        - 支援多選月份選擇器（periods）進行並列比較
        - 支援單選月份選擇器（period）或日期選擇器（startDate/endDate）
        - 可提供快速選擇：本月、本季、本年等期間
        """)
    public ResponseEntity<List<ComprehensiveIncomeStatementDto>> getComprehensiveIncomeStatement(
            @ParameterObject ComprehensiveIncomeStatementQueryDto query,
            @RequestParam(required = false) String periods) {

        // 處理逗號分隔的 periods 參數（如果提供）
        if (periods != null && !periods.isBlank() 
                && (query.getPeriods() == null || query.getPeriods().isEmpty())) {
            List<String> periodsList = List.of(periods.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
            query.setPeriods(periodsList);
        }

        List<ComprehensiveIncomeStatementDto> report;

        // 優先使用 periods（多個月份並列比較）
        List<String> periodsList = query.getPeriodsList();
        if (periodsList != null && periodsList.size() > 1) {
            // 多個月份查詢
            report = comprehensiveIncomeStatementService.generateComprehensiveIncomeStatement(periodsList);
        } else if (periodsList != null && periodsList.size() == 1) {
            // 單一月份（從 periods 轉換）
            report = comprehensiveIncomeStatementService.generateComprehensiveIncomeStatement(
                    periodsList.get(0),
                    query.getStartDate(),
                    query.getEndDate());
        } else {
            // 單一月份或日期區間查詢（原有邏輯）
            report = comprehensiveIncomeStatementService.generateComprehensiveIncomeStatement(
                    query.getPeriod(),
                    query.getStartDate(),
                    query.getEndDate());
        }

        return ResponseEntity.ok(report);
    }
}

