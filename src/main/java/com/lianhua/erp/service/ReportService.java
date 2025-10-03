package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.*;

import java.util.List;

public interface ReportService {
    // 供應商相關
    List<SupplierBalanceReportDto> getSupplierBalances();
    List<SupplierMonthlyReportDto> getSupplierMonthlyReports();
    List<PurchasePaymentStatusDto> getPurchasePaymentStatuses();
    List<SupplierBalanceReportDto> getUnpaidSuppliers();

    // 員工薪資相關
    List<MonthlySalaryReportDto> getMonthlySalaryExpenses();
    List<EmployeeYearlySalaryReportDto> getEmployeeYearlySalaries();
    List<MonthlySalaryDetailDto> getMonthlySalaryDetails(String month);

    // 营業支出相關
    List<OperatingCostReportDto> getMonthlyOperatingCosts();
    List<OperatingCostCategoryDto> getOperatingCostByCategory();
    List<OperatingCostMonthlyCategoryDto> getMonthlyCategoryOperatingCosts();





}
