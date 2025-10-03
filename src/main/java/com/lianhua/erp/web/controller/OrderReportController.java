package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.report.OrderReportDto;
import com.lianhua.erp.dto.report.ProductSalesReportDto;
import com.lianhua.erp.service.OrderReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports/orders")
@Tag(name = "訂單報表", description = "提供訂單與銷售的統計報表 API")
public class OrderReportController {

    private final OrderReportService reportService;

    public OrderReportController(OrderReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/annual/{year}")
    @Operation(summary = "年度客戶訂單統計")
    public ResponseEntity<List<OrderReportDto>> getAnnualSummary(@PathVariable int year) {
        return ResponseEntity.ok(reportService.getAnnualSummary(year));
    }

    @GetMapping("/monthly/{year}")
    @Operation(summary = "每月客戶訂單統計")
    public ResponseEntity<List<OrderReportDto>> getMonthlySummary(@PathVariable int year) {
        return ResponseEntity.ok(reportService.getMonthlySummary(year));
    }

    @GetMapping("/outstanding/{months}")
    @Operation(summary = "應收帳款（近N個月未結清）")
    public ResponseEntity<List<OrderReportDto>> getOutstandingOrders(@PathVariable int months) {
        return ResponseEntity.ok(reportService.getOutstandingOrders(months));
    }

    @GetMapping("/top-products/{year}")
    @Operation(summary = "熱銷商品排行")
    public ResponseEntity<List<ProductSalesReportDto>> getTopSellingProducts(@PathVariable int year) {
        return ResponseEntity.ok(reportService.getTopSellingProducts(year));
    }
}
