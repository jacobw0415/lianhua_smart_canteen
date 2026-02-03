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
        // 增加一個對 Timestamp 的處理，預防某些環境下的行為
        if (obj instanceof java.sql.Timestamp) return ((java.sql.Timestamp) obj).toLocalDateTime().toLocalDate();
        return LocalDate.parse(obj.toString());
    }

    private BigDecimal parseBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        return new BigDecimal(obj.toString());
    }

    // =========================================================
    // 2. 核心決策圖表 (v3.0 財務三表與深度分析映射)
    // =========================================================

    /** [圖表 1] 損益平衡分析：映射累計營收、累計支出與平衡門檻 */
    @Override
    public List<BreakEvenPointDto> getBreakEvenAnalysis(String period) {
        return dashboardRepository.getBreakEvenData(period).stream()
                .map(row -> new BreakEvenPointDto(
                        parseLocalDate(row[0]),     // date
                        parseBigDecimal(row[1]),    // runningRevenue
                        parseBigDecimal(row[2]),    // runningExpense
                        parseBigDecimal(row[3])     // breakEvenThreshold
                )).collect(Collectors.toList());
    }

    /** [圖表 2] 流動性指標：單行數據映射 */
    @Override
    public LiquidityDto getLiquidityAnalytics() {
        return dashboardRepository.getLiquidityMetrics().stream()
                .findFirst()
                .map(row -> new LiquidityDto(
                        parseBigDecimal(row[0]),    // liquidAssets
                        parseBigDecimal(row[1]),    // liquidLiabilities
                        parseBigDecimal(row[2])     // quickAssets
                )).orElse(new LiquidityDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    /** [圖表 3] 未來現金流預測：30 天數據映射 */
    @Override
    public List<CashflowForecastDto> getCashflowForecast(LocalDate baseDate, int days) {
        return dashboardRepository.getCashflowForecast(baseDate, days).stream()
                .map(row -> {
                    var date = parseLocalDate(row[0]);
                    var inflow = parseBigDecimal(row[1]);
                    var outflow = parseBigDecimal(row[2]);
                    var net = (inflow != null && outflow != null)
                            ? inflow.subtract(outflow)
                            : null;
                    return new CashflowForecastDto(date, inflow, outflow, net);
                })
                .collect(Collectors.toList());
    }

    /** [圖表 4] 商品獲利 Pareto 分析：名稱、金額、累計百分比 */
    @Override
    public List<ProductParetoDto> getProductParetoAnalysis(LocalDate start, LocalDate end) {
        return dashboardRepository.getProductPareto(start, end).stream()
                .map(row -> new ProductParetoDto(
                        (String) row[0],            // productName
                        parseBigDecimal(row[1]),    // totalAmount
                        parseBigDecimal(row[2]).doubleValue() // cumulativePct
                )).collect(Collectors.toList());
    }

    /** [圖表 5] 供應商採購集中度分析 */
    @Override
    public List<SupplierConcentrationDto> getSupplierConcentration(LocalDate start, LocalDate end) {
        return dashboardRepository.getSupplierConcentration(start, end).stream()
                .map(row -> new SupplierConcentrationDto(
                        (String) row[0],            // supplierName
                        parseBigDecimal(row[1]).doubleValue(), // ratio
                        parseBigDecimal(row[2])     // totalAmount
                )).collect(Collectors.toList());
    }

    /** [圖表 6] 客戶回購與沉睡分析 */
    @Override
    public List<CustomerRetentionDto> getCustomerRetention() {
        return dashboardRepository.getCustomerRetention().stream()
                .map(row -> new CustomerRetentionDto(
                        (String) row[0],            // customerName
                        parseLocalDate(row[1]),     // lastOrderDate
                        ((Number) row[2]).intValue(), // daysSinceLastOrder
                        (String) row[3]             // status (活躍/風險/流失)
                )).collect(Collectors.toList());
    }

    /** [圖表 7] 採購結構分析 (按品項) */
    @Override
    public List<PurchaseStructureDto> getPurchaseStructureByItem(LocalDate start, LocalDate end) {
        return dashboardRepository.getPurchaseStructureByItem(start, end).stream()
                .map(row -> new PurchaseStructureDto(
                        (String) row[0],           // item name
                        parseBigDecimal(row[1])    // totalAmount
                )).collect(Collectors.toList());
    }

    /**
     * [圖表 9] 客戶採購集中度分析實作
     * 核心邏輯：計算各客戶在指定期間內的訂單總額及其佔全體營收的比例。
     */
    @Override
    public List<CustomerConcentrationDto> getCustomerConcentration(LocalDate startDate, LocalDate endDate) {
        // 1. 執行 Repository 原生查詢
        List<Object[]> results = dashboardRepository.getCustomerConcentration(startDate, endDate);

        // 2. 將查詢結果 Object[] 映射至 DTO
        return results.stream()
                .map(row -> new CustomerConcentrationDto(
                        (String) row[0],              // oc.name: 客戶名稱
                        ((Number) row[1]).doubleValue(), // ratio: 百分比佔比
                        parseBigDecimal(row[2])       // totalAmount: 訂單總金額
                ))
                .collect(Collectors.toList());
    }

}