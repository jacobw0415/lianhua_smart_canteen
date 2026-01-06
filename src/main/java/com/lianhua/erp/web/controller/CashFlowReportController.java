package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.report.CashFlowReportDto;
import com.lianhua.erp.dto.report.CashFlowReportQueryDto;
import com.lianhua.erp.service.CashFlowReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 💰 現金流量表報表 Controller
 * 
 * API 路徑說明：
 * - 前端使用：<Resource name="cash_flow_reports" list={CashFlowReportList} />
 * 
 * 提供現金流量統計報表的 API 端點
 * 支援依月份或日期區間查詢現金流入與流出
 * 
 * 前端時間選擇器建議：
 * - 月份選擇器：傳遞 period 參數（YYYY-MM）
 * - 日期區間選擇器：傳遞 startDate 和 endDate 參數
 * - 快速選擇：本週、本月、本季、本年等預設區間
 */
@RestController
@RequestMapping("/api/cash_flow_reports")
@RequiredArgsConstructor
@Tag(name = "現金流量表", description = "現金流量報表 API - 統計現金流入與流出")
public class CashFlowReportController {

  private final CashFlowReportService cashFlowReportService;

  /**
   * 📊 生成現金流量報表
   * 
   * 支援三種查詢方式：
   * 1. 指定月份：period=2025-10
   * 2. 日期區間：startDate=2025-01-01&endDate=2025-12-31
   * 3. 全部資料：不傳任何參數
   * 
   * @param query 查詢條件（包含 period、startDate、endDate）
   * @return 現金流量報表資料列表（包含各期間明細及合計）
   */
  @GetMapping
  @Operation(summary = "查詢現金流量報表", description = """
      查詢現金流量報表，支援以下查詢方式：
      1. 指定月份：period=2025-10
      2. 日期區間：startDate=2025-01-01&endDate=2025-12-31
      3. 全部資料：不傳任何參數

      報表包含：
      - 零售現金收入 (Sales)
      - 訂單收款收入 (Receipts)
      - 採購付款支出 (Payments)
      - 營運費用支出 (Expenses)
      - 總流入金額、總流出金額、淨現金流

      💡 前端時間選擇器建議：
      - 使用 @ParameterObject 自動綁定查詢參數
      - 支援月份選擇器（period）或日期區間選擇器（startDate/endDate）
      - 可提供快速選擇：本週、本月、本季、本年等
      """)
  public ResponseEntity<List<CashFlowReportDto>> getCashFlowReport(
      @ParameterObject CashFlowReportQueryDto query) {
    List<CashFlowReportDto> report = cashFlowReportService.generateCashFlow(
        query.getPeriod(),
        query.getStartDate(),
        query.getEndDate());
    return ResponseEntity.ok(report);
  }
}
