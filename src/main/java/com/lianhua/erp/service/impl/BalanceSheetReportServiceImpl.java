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
 * 
 * 資產負債表是「時點報表」，顯示截止至指定月底或日期的累積餘額。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceSheetReportServiceImpl implements BalanceSheetReportService {

    private final BalanceSheetReportRepository repository;

    @Override
    public List<BalanceSheetReportDto> generateBalanceSheet(String period) {
        return generateBalanceSheet(period, null);
    }

    @Override
    public List<BalanceSheetReportDto> generateBalanceSheet(String period, String endDate) {

        List<BalanceSheetReportDto> list = repository.getBalanceSheetList(period, endDate);
        if (list == null || list.isEmpty()) {
            return list;
        }

        // 🧮 自動加上「合計」列（同 CashFlowReport 結構）
        BalanceSheetReportDto total = new BalanceSheetReportDto();
        String label = "合計";

        if (endDate != null && !endDate.isBlank()) {
            label += String.format(" (截止至 %s)", endDate);
        } else if (period != null && !period.isBlank()) {
            label += String.format(" (截止至 %s)", period);
        }

        total.setAccountingPeriod(label);

        // 累加各主要科目
        total.setAccountsReceivable(sum(list, BalanceSheetReportDto::getAccountsReceivable));
        total.setAccountsPayable(sum(list, BalanceSheetReportDto::getAccountsPayable));
        total.setCash(sum(list, BalanceSheetReportDto::getCash));

        // 計算總資產、總負債與權益（基於合計行的基礎數據重新計算）
        // 注意：合計行的 total_assets = 合計的 accounts_receivable + 合計的 cash
        // 這樣計算更清晰，且與各期間的計算邏輯一致
        BigDecimal totalAssets = total.getAccountsReceivable().add(total.getCash());
        BigDecimal totalLiabilities = total.getAccountsPayable();
        BigDecimal equity = totalAssets.subtract(totalLiabilities);

        total.setTotalAssets(totalAssets);
        total.setTotalLiabilities(totalLiabilities);
        total.setEquity(equity);

        // ✅ 避免重複加入
        boolean hasTotal = list.stream()
                .anyMatch(dto -> dto.getAccountingPeriod() != null && dto.getAccountingPeriod().startsWith("合計"));
        if (!hasTotal) {
            list.add(total);
        }

        return list;
    }

    @Override
    public List<BalanceSheetReportDto> generateBalanceSheet(List<String> periods) {
        if (periods == null || periods.isEmpty()) {
            return List.of();
        }

        // 查詢多個月份的資產負債表
        List<BalanceSheetReportDto> list = repository.getBalanceSheetList(periods);
        if (list == null || list.isEmpty()) {
            return list;
        }

        // 🧮 自動加上「合計」列
        // 注意：資產負債表是時點報表，多個時點的「合計」在會計上沒有意義
        // 但為了報表完整性，我們顯示最後一個期間的值作為參考
        BalanceSheetReportDto total = new BalanceSheetReportDto();
        String label = "合計";

        // 如果有多個期間，顯示範圍和最後一個期間
        if (periods.size() > 1) {
            String firstPeriod = periods.get(0);
            String lastPeriod = periods.get(periods.size() - 1);
            label += String.format(" (%s 至 %s，顯示最後期間值)", firstPeriod, lastPeriod);

            // 使用最後一個期間的值（更符合時點報表的特性）
            BalanceSheetReportDto lastPeriodDto = list.get(list.size() - 1);
            total.setAccountsReceivable(lastPeriodDto.getAccountsReceivable());
            total.setAccountsPayable(lastPeriodDto.getAccountsPayable());
            total.setCash(lastPeriodDto.getCash());
            total.setTotalAssets(lastPeriodDto.getTotalAssets());
            total.setTotalLiabilities(lastPeriodDto.getTotalLiabilities());
            total.setEquity(lastPeriodDto.getEquity());
        } else if (periods.size() == 1) {
            label += String.format(" (截止至 %s)", periods.get(0));
            // 單一期間，合計等於該期間的值
            BalanceSheetReportDto singleDto = list.get(0);
            total.setAccountsReceivable(singleDto.getAccountsReceivable());
            total.setAccountsPayable(singleDto.getAccountsPayable());
            total.setCash(singleDto.getCash());
            total.setTotalAssets(singleDto.getTotalAssets());
            total.setTotalLiabilities(singleDto.getTotalLiabilities());
            total.setEquity(singleDto.getEquity());
        } else {
            total.setAccountsReceivable(BigDecimal.ZERO);
            total.setAccountsPayable(BigDecimal.ZERO);
            total.setCash(BigDecimal.ZERO);
            total.setTotalAssets(BigDecimal.ZERO);
            total.setTotalLiabilities(BigDecimal.ZERO);
            total.setEquity(BigDecimal.ZERO);
        }

        total.setAccountingPeriod(label);

        // ✅ 避免重複加入
        boolean hasTotal = list.stream()
                .anyMatch(dto -> dto.getAccountingPeriod() != null && dto.getAccountingPeriod().startsWith("合計"));
        if (!hasTotal) {
            list.add(total);
        }

        return list;
    }

    /**
     * 🔧 BigDecimal 累加工具
     */
    private BigDecimal sum(List<BalanceSheetReportDto> list,
            java.util.function.Function<BalanceSheetReportDto, BigDecimal> getter) {
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
