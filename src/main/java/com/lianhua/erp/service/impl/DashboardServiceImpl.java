package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.dashboard.*;
import com.lianhua.erp.dto.dashboard.analytics.*;
import com.lianhua.erp.repository.DashboardRepository;
import com.lianhua.erp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final DashboardRepository dashboardRepository;

    // =========================================================
    // 1. 核心 KPI 摘要
    // =========================================================
    @Override
    public DashboardStatsDto getDashboardStats() {
        LocalDate today = LocalDate.now();
        String currentPeriod = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // 營運概況
        BigDecimal todaySales = dashboardRepository.getTodaySalesTotal(today);
        BigDecimal monthSales = dashboardRepository.getMonthSalesTotal(currentPeriod);
        BigDecimal monthPurchase = dashboardRepository.getMonthPurchaseTotal(currentPeriod);
        BigDecimal monthExpense = dashboardRepository.getMonthExpenseTotal(currentPeriod);

        // 財務指標 (已優化 JOIN 查詢)
        BigDecimal accountsReceivable = dashboardRepository.getAccountsReceivableTotal();
        BigDecimal accountsPayable = dashboardRepository.getAccountsPayableTotal();
        BigDecimal upcomingAR = dashboardRepository.getUpcomingAR();

        // 業務統計
        long suppliers = dashboardRepository.countActiveSuppliers();
        long customers = dashboardRepository.countTotalCustomers();
        long products = dashboardRepository.countActiveProducts();
        int pendingOrders = dashboardRepository.getPendingOrderCount();

        // 現金流量
        BigDecimal todayReceiptsTotal = dashboardRepository.getTodayReceiptsTotal(today);
        BigDecimal todayTotalInflow = dashboardRepository.getTodayTotalInflow(today);
        BigDecimal monthTotalReceived = dashboardRepository.getMonthTotalReceived(currentPeriod);

        // 淨利與利潤率計算
        BigDecimal netProfit = monthSales.subtract(monthPurchase).subtract(monthExpense);
        double profitMargin = monthSales.compareTo(BigDecimal.ZERO) > 0
                ? netProfit.divide(monthSales, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0;

        return new DashboardStatsDto(
                todaySales, monthSales, monthPurchase, monthExpense,
                suppliers, customers, products, pendingOrders,
                accountsPayable, accountsReceivable, netProfit, profitMargin,
                todayReceiptsTotal, todayTotalInflow, monthTotalReceived, upcomingAR
        );
    }

    // =========================================================
    // 2. 趨勢與基礎圖表
    // =========================================================
    @Override
    public List<TrendPointDto> getSalesTrendData(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return dashboardRepository.getCombinedTrend(startDate).stream()
                .map(row -> new TrendPointDto(
                        parseLocalDate(row[0]),    // date
                        parseBigDecimal(row[1]),   // saleAmount
                        parseBigDecimal(row[2])    // receiptAmount
                )).collect(Collectors.toList());
    }

    @Override
    public List<ExpenseCompositionDto> getExpenseComposition() {
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return dashboardRepository.getMonthlyExpenseComposition(period).stream()
                .map(row -> new ExpenseCompositionDto(
                        (String) row[0],           // category name
                        parseBigDecimal(row[1])    // amount
                )).collect(Collectors.toList());
    }

    // =========================================================
    // 3. 進階分析 API (v2.0 決策支援)
    // =========================================================

    /** 分析 AR/AP 逾期風險帳齡 */
    @Override
    public List<AccountAgingDto> getAgingAnalytics() {
        return dashboardRepository.getAccountAging().stream()
                .map(row -> new AccountAgingDto(
                        (String) row[0],           // bucketLabel
                        parseBigDecimal(row[1]),   // arAmount
                        parseBigDecimal(row[2])    // apAmount
                )).collect(Collectors.toList());
    }

    /** 獲取損益四線走勢 (包含營收、毛利、費用、淨利) */
    @Override
    public List<ProfitLossPointDto> getProfitLossTrend(int months) {
        // 注意：Repository SQL 內部已有 LIMIT 6，此處 months 可用於後續動態擴展
        return dashboardRepository.getProfitLossTrend().stream()
                .map(row -> new ProfitLossPointDto(
                        (String) row[0],           // period
                        parseBigDecimal(row[1]),   // revenue
                        parseBigDecimal(row[2]),   // grossProfit
                        parseBigDecimal(row[3]),   // expense
                        parseBigDecimal(row[4])    // netProfit
                )).collect(Collectors.toList());
    }

    /** 訂單轉化漏斗分析 */
    @Override
    public List<OrderFunnelDto> getOrderFunnel(String period) {
        return dashboardRepository.getOrderFunnel().stream()
                .map(row -> new OrderFunnelDto(
                        (String) row[0],           // stage (status)
                        ((Number) row[1]).intValue(), // orderCount
                        parseBigDecimal(row[2])    // totalAmount
                )).collect(Collectors.toList());
    }

    /** 獲取待辦任務 (即將到期的 AR 帳單清單) */
    @Override
    public List<DashboardTaskDto> getPendingTasks() {
        // SQL 回傳順序: oc.name(0), o.order_no(1), o.delivery_date(2), balance(3)
        return dashboardRepository.getUpcomingARList().stream()
                .map(row -> new DashboardTaskDto(
                        "AR_DUE",                  // task type
                        (String) row[0],           // customer name
                        (String) row[1],           // order_no
                        parseBigDecimal(row[3]),   // balance
                        parseLocalDate(row[2])     // due date
                )).collect(Collectors.toList());
    }

    // =========================================================
    // 工具方法：安全轉換 (處理 Native Query 的各類回傳格式)
    // =========================================================
    private LocalDate parseLocalDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof java.sql.Date) return ((java.sql.Date) obj).toLocalDate();
        if (obj instanceof java.time.LocalDate) return (LocalDate) obj;
        return LocalDate.parse(obj.toString());
    }

    private BigDecimal parseBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        return new BigDecimal(obj.toString());
    }
}