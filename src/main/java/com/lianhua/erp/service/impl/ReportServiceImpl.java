package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.report.ProfitReportDto;
import com.lianhua.erp.repository.ReportRepository;
import com.lianhua.erp.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 💰 損益報表 Service 實作
 * 支援月份與日期區間查詢，結構統一與資產負債表。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository repository;

    @Override
    public List<ProfitReportDto> getMonthlyProfitReport(String period, String startDate, String endDate) {

        // 📊 從 Repository 查詢報表資料
        List<ProfitReportDto> list = repository.getProfitReport(period, startDate, endDate);

        if (list == null || list.isEmpty()) {
            return list;
        }

        // 🧮 新增「合計」列
        ProfitReportDto total = new ProfitReportDto();
        String label = "合計";

        if (startDate != null && endDate != null) {
            label += STR." (\{startDate} ~ \{endDate})";
        } else if (period != null && !period.isBlank()) {
            label += STR." (\{period})";
        }

        total.setAccountingPeriod(label);

        // 🔹 累計加總欄位
        total.setTotalSales(sum(list, ProfitReportDto::getTotalSales));
        total.setTotalOrders(sum(list, ProfitReportDto::getTotalOrders));
        total.setTotalRevenue(sum(list, ProfitReportDto::getTotalRevenue));
        total.setTotalPurchase(sum(list, ProfitReportDto::getTotalPurchase));
        total.setTotalExpense(sum(list, ProfitReportDto::getTotalExpense));

        // 🔹 計算本期淨利
        total.setNetProfit(
                total.getTotalRevenue()
                        .subtract(total.getTotalPurchase())
                        .subtract(total.getTotalExpense())
        );

        list.add(total);
        return list;
    }

    /**
     * BigDecimal 累加工具
     */
    private BigDecimal sum(List<ProfitReportDto> list, java.util.function.Function<ProfitReportDto, BigDecimal> getter) {
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
