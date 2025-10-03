package com.lianhua.erp.service;

import com.lianhua.erp.dto.report.OrderReportDto;
import com.lianhua.erp.dto.report.ProductSalesReportDto;

import java.util.List;

public interface OrderReportService {
    List<OrderReportDto> getAnnualSummary(int year);
    List<OrderReportDto> getMonthlySummary(int year);
    List<OrderReportDto> getOutstandingOrders(int months);
    List<ProductSalesReportDto> getTopSellingProducts(int year);
}

