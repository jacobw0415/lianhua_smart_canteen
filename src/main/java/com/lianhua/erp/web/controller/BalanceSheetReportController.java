package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.report.BalanceSheetReportDto;
import com.lianhua.erp.dto.report.BalanceSheetReportQueryDto;
import com.lianhua.erp.service.BalanceSheetReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 💼 資產負債表報表 Controller
 * 
 * API 路徑說明：
 * -
 * 前端使用：<Resource name="balance_sheet_reports" list={BalanceSheetReportList} />
 * 
 * 提供資產負債表統計報表的 API 端點
 * 資產負債表是「時點報表」，顯示截止至指定月底或日期的累積餘額
 * 
 * 前端時間選擇器建議：
 * - 月份選擇器：傳遞 period 參數（YYYY-MM），查詢截止至該月底的累積餘額
 * - 日期選擇器：傳遞 endDate 參數（yyyy-MM-dd），查詢截止至該日期的累積餘額
 * - 快速選擇：本月、本季、本年等預設時點
 */
@RestController
@RequestMapping("/api/reports/balance_sheet")
@RequiredArgsConstructor
@Tag(name = "資產負債表", description = "資產負債表報表 API - 統計資產、負債與權益")
public class BalanceSheetReportController {

  private final BalanceSheetReportService balanceSheetReportService;

  /**
   * 📊 生成資產負債表報表（時點報表）
   * 
   * 資產負債表顯示截止至指定月底或日期的累積餘額（存量概念）。
   * 
   * 支援多種查詢方式：
   * 1. 多個月份：periods=2025-10,2025-11,2025-12（查詢多個月份並列比較）
   * 2. 指定月份：period=2025-10（查詢截止至2025-10月底的累積餘額）
   * 3. 指定日期：endDate=2025-12-31（查詢截止至2025-12-31的累積餘額）
   * 
   * @param query 查詢條件（包含 periods、period 或 endDate）
   * @return 資產負債表報表資料列表（包含各期間明細及合計）
   */
  @GetMapping
  @Operation(summary = "查詢資產負債表（時點報表）", description = """
      查詢資產負債表，顯示截止至指定月底或日期的累積餘額。

      支援以下查詢方式：
      1. 多個月份：periods=2025-10,2025-11,2025-12（查詢多個月份並列比較）⭐ 新增
      2. 指定月份：period=2025-10（查詢截止至2025-10月底的累積餘額）
      3. 指定日期：endDate=2025-12-31（查詢截止至2025-12-31的累積餘額）

      查詢優先級：
      - 如果提供 periods，優先使用（忽略 period 和 endDate）
      - 如果提供 period，使用單一月份查詢
      - 如果提供 endDate，使用日期查詢

      報表包含：
      - 應收帳款（截止至指定時點的未收客戶款累積餘額）
      - 現金（截止至指定時點的累積現金餘額 = 收入 - 支出）
      - 應付帳款（截止至指定時點的未付供應商款累積餘額）
      - 總資產、總負債、業主權益（淨值）

      💡 前端時間選擇器建議：
      - 使用 @ParameterObject 自動綁定查詢參數
      - 支援多選月份選擇器（periods）進行並列比較
      - 支援單選月份選擇器（period）或日期選擇器（endDate）
      - 可提供快速選擇：本月、本季、本年等時點
      """)
  public ResponseEntity<List<BalanceSheetReportDto>> getBalanceSheetReport(
      @ParameterObject BalanceSheetReportQueryDto query,
      @RequestParam(required = false) String periods) {

    // 處理逗號分隔的 periods 參數（如果提供）
    if (periods != null && !periods.isBlank() && (query.getPeriods() == null || query.getPeriods().isEmpty())) {
      List<String> periodsList = List.of(periods.split(","))
          .stream()
          .map(String::trim)
          .filter(s -> !s.isBlank())
          .toList();
      query.setPeriods(periodsList);
    }

    List<BalanceSheetReportDto> report;

    // 優先使用 periods（多個月份並列比較）
    List<String> periodsList = query.getPeriodsList();
    if (periodsList != null && periodsList.size() > 1) {
      // 多個月份查詢
      report = balanceSheetReportService.generateBalanceSheet(periodsList);
    } else if (periodsList != null && periodsList.size() == 1) {
      // 單一月份（從 periods 轉換）
      report = balanceSheetReportService.generateBalanceSheet(
          periodsList.get(0),
          query.getEndDate());
    } else {
      // 單一月份或日期查詢（原有邏輯）
      report = balanceSheetReportService.generateBalanceSheet(
          query.getPeriod(),
          query.getEndDate());
    }

    return ResponseEntity.ok(report);
  }
}
