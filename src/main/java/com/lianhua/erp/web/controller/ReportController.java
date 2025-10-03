package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.report.*;
import com.lianhua.erp.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "報表管理", description = "供應商/採購/付款/薪資報表 API")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ==== 供應商相關 ====

    @GetMapping("/supplier-balances")
    @Operation(summary = "供應商應付款餘額報表", description = "顯示每個供應商的採購、付款與餘額")
    public List<SupplierBalanceReportDto> getSupplierBalances() {
        return reportService.getSupplierBalances();
    }

    @GetMapping("/supplier-monthly")
    @Operation(summary = "供應商月度對帳報表", description = "按月份顯示採購、付款、餘額")
    public List<SupplierMonthlyReportDto> getSupplierMonthlyReports() {
        return reportService.getSupplierMonthlyReports();
    }

    @GetMapping("/purchase-status")
    @Operation(summary = "採購付款狀態報表", description = "顯示單筆採購的付款與未付款情況")
    public List<PurchasePaymentStatusDto> getPurchasePaymentStatuses() {
        return reportService.getPurchasePaymentStatuses();
    }

    @GetMapping("/unpaid-suppliers")
    @Operation(summary = "未付款供應商清單", description = "列出有欠款的供應商")
    public List<SupplierBalanceReportDto> getUnpaidSuppliers() {
        return reportService.getUnpaidSuppliers();
    }

    // ==== 員工薪資相關 ====

    @GetMapping("/salary/monthly")
    @Operation(summary = "每月薪資支出報表")
    public List<MonthlySalaryReportDto> getMonthlySalaryExpenses() {
        return reportService.getMonthlySalaryExpenses();
    }

    @GetMapping("/salary/yearly")
    @Operation(summary = "各員工年度薪資總額報表")
    public List<EmployeeYearlySalaryReportDto> getEmployeeYearlySalaries() {
        return reportService.getEmployeeYearlySalaries();
    }

    @GetMapping("/salary/details/{month}")
    @Operation(summary = "當月薪資支出明細（附員工姓名）")
    public List<MonthlySalaryDetailDto> getMonthlySalaryDetails(@PathVariable String month) {
        return reportService.getMonthlySalaryDetails(month);
    }

    //
    @GetMapping("/operating-costs/monthly")
    @Operation(summary = "每月營運成本總額")
    public List<OperatingCostReportDto> getMonthlyOperatingCosts() {
        return reportService.getMonthlyOperatingCosts();
    }

    @GetMapping("/operating-costs/category")
    @Operation(summary = "營運成本按類別統計")
    public List<OperatingCostCategoryDto> getOperatingCostByCategory() {
        return reportService.getOperatingCostByCategory();
    }

    @GetMapping("/operating-costs/monthly-category")
    @Operation(summary = "每月各類別營運成本")
    public List<OperatingCostMonthlyCategoryDto> getMonthlyCategoryOperatingCosts() {
        return reportService.getMonthlyCategoryOperatingCosts();
    }

}
