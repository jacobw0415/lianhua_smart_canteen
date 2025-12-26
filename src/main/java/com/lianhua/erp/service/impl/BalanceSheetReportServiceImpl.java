package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.report.BalanceSheetReportDto;
import com.lianhua.erp.repository.BalanceSheetReportRepository;
import com.lianhua.erp.service.BalanceSheetReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 💼 資產負債表 Service 實作
 * 對應新版 Repository，可同時支援月份與日期區間查詢。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceSheetReportServiceImpl implements BalanceSheetReportService {

    private final BalanceSheetReportRepository repository;

    @Override
    public List<BalanceSheetReportDto> generateBalanceSheet(String period) {
        return generateBalanceSheet(period, null, null);
    }

    @Override
    public List<BalanceSheetReportDto> generateBalanceSheet(String period, String startDate, String endDate) {

        List<BalanceSheetReportDto> list = repository.getBalanceSheet(period, startDate, endDate);
        if (list == null || list.isEmpty()) {
            return list;
        }

        // 🧮 自動加上「合計」列（同 CashFlowReport 結構）
        BalanceSheetReportDto total = new BalanceSheetReportDto();
        String label = "合計";

        if (startDate != null && endDate != null) {
            label += String.format(" (%s ~ %s)", startDate, endDate);
        } else if (period != null && !period.isBlank()) {
            label += String.format(" (%s)", period);
        }

        total.setAccountingPeriod(label);

        // 累加各主要科目
        total.setAccountsReceivable(sum(list, BalanceSheetReportDto::getAccountsReceivable));
        total.setAccountsPayable(sum(list, BalanceSheetReportDto::getAccountsPayable));
        total.setCash(sum(list, BalanceSheetReportDto::getCash));

        // 計算總資產、總負債與權益
        BigDecimal totalAssets = total.getAccountsReceivable()
                .add(total.getCash());
        BigDecimal totalLiabilities = total.getAccountsPayable();
        BigDecimal equity = totalAssets.subtract(totalLiabilities);

        total.setTotalAssets(totalAssets);
        total.setTotalLiabilities(totalLiabilities);
        total.setEquity(equity);

        list.add(total);
        return list;
    }

    /**
     * 🔧 BigDecimal 累加工具
     */
    private BigDecimal sum(List<BalanceSheetReportDto> list, java.util.function.Function<BalanceSheetReportDto, BigDecimal> getter) {
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
