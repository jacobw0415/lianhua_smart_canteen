package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.dashboard.DashboardStatsDto;
import com.lianhua.erp.dto.dashboard.TrendPointDto;
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

    @Override
    public DashboardStatsDto getDashboardStats() {
        LocalDate today = LocalDate.now();
        // 依照 Schema v2.7 規範，會計期間格式為 YYYY-MM
        String currentPeriod = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // 1. 從 Repository 獲取原始數據 (排除作廢單據)
        BigDecimal todaySales = dashboardRepository.getTodaySalesTotal(today);
        BigDecimal monthSales = dashboardRepository.getMonthSalesTotal(currentPeriod);
        BigDecimal monthPurchase = dashboardRepository.getMonthPurchaseTotal(currentPeriod);
        BigDecimal monthExpense = dashboardRepository.getMonthExpenseTotal(currentPeriod);

        BigDecimal accountsReceivable = dashboardRepository.getAccountsReceivableTotal();
        BigDecimal accountsPayable = dashboardRepository.getAccountsPayableTotal();

        long suppliers = dashboardRepository.countActiveSuppliers();
        long customers = dashboardRepository.countTotalCustomers();
        long products = dashboardRepository.countActiveProducts();
        int pendingOrders = dashboardRepository.getPendingOrderCount();

        BigDecimal todayReceiptsTotal = dashboardRepository.getTodayReceiptsTotal(today); // 今日訂單收款
        BigDecimal todayTotalInflow = dashboardRepository.getTodayTotalInflow(today);      // 今日總入金
        BigDecimal monthTotalReceived = dashboardRepository.getMonthTotalReceived(currentPeriod); // 本月累計實收
        BigDecimal upcomingAR = dashboardRepository.getUpcomingAR();

        // 2. 二次計算指標 (利潤與利率)
        // 淨利 = 本月銷售 - 本月採購 - 本月費用
        BigDecimal netProfit = monthSales.subtract(monthPurchase).subtract(monthExpense);

        // 淨利率 = (淨利 / 本月銷售) * 100
        double profitMargin = 0;
        if (monthSales.compareTo(BigDecimal.ZERO) > 0) {
            profitMargin = netProfit.divide(monthSales, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // 3. 封裝並回傳 DTO
        return new DashboardStatsDto(
                todaySales,
                monthSales,
                monthPurchase,
                monthExpense,
                suppliers,
                customers,
                products,
                pendingOrders,
                accountsPayable,
                accountsReceivable,
                netProfit,
                profitMargin,
                todayReceiptsTotal,
                todayTotalInflow,
                monthTotalReceived,
                upcomingAR
        );
    }

    @Override
    public List<TrendPointDto> getSalesTrendData(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<Object[]> results = dashboardRepository.getDailySalesTrend(startDate);

        return results.stream().map(result -> new TrendPointDto(
                ((java.sql.Date) result[0]).toLocalDate(),
                (BigDecimal) result[1],
                (String) result[2]
        )).collect(Collectors.toList());
    }
}