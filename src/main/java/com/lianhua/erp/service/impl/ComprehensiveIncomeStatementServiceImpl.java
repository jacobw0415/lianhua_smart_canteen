package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.report.ComprehensiveIncomeStatementDto;
import com.lianhua.erp.repository.ComprehensiveIncomeStatementRepository;
import com.lianhua.erp.service.ComprehensiveIncomeStatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 💼 綜合損益表服務實作
 * 
 * 負責彙總並計算綜合損益表的各項指標。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprehensiveIncomeStatementServiceImpl implements ComprehensiveIncomeStatementService {

    private final ComprehensiveIncomeStatementRepository repository;

    @Override
    public List<ComprehensiveIncomeStatementDto> generateComprehensiveIncomeStatement(String period) {
        return generateComprehensiveIncomeStatement(period, null, null);
    }

    @Override
    public List<ComprehensiveIncomeStatementDto> generateComprehensiveIncomeStatement(
            String period, String startDate, String endDate) {

        log.info("生成綜合損益表：period={}, startDate={}, endDate={}", period, startDate, endDate);

        List<ComprehensiveIncomeStatementDto> list = repository.getComprehensiveIncomeStatement(
                period, startDate, endDate);

        if (list == null || list.isEmpty()) {
            return list;
        }

        // 🧮 新增「合計」列
        ComprehensiveIncomeStatementDto total = new ComprehensiveIncomeStatementDto();
        String label = "合計";

        if (startDate != null && endDate != null) {
            label += String.format(" (%s ~ %s)", startDate, endDate);
        } else if (period != null && !period.isBlank()) {
            label += String.format(" (%s)", period);
        }

        total.setAccountingPeriod(label);

        // 🔹 累計加總欄位
        total.setRetailSales(sum(list, ComprehensiveIncomeStatementDto::getRetailSales));
        total.setOrderSales(sum(list, ComprehensiveIncomeStatementDto::getOrderSales));
        total.setTotalRevenue(sum(list, ComprehensiveIncomeStatementDto::getTotalRevenue));
        total.setCostOfGoodsSold(sum(list, ComprehensiveIncomeStatementDto::getCostOfGoodsSold));
        total.setGrossProfit(sum(list, ComprehensiveIncomeStatementDto::getGrossProfit));
        total.setTotalOperatingExpenses(sum(list, ComprehensiveIncomeStatementDto::getTotalOperatingExpenses));
        total.setOperatingProfit(sum(list, ComprehensiveIncomeStatementDto::getOperatingProfit));
        total.setOtherIncome(sum(list, ComprehensiveIncomeStatementDto::getOtherIncome));
        total.setOtherExpenses(sum(list, ComprehensiveIncomeStatementDto::getOtherExpenses));
        total.setNetProfit(sum(list, ComprehensiveIncomeStatementDto::getNetProfit));
        total.setOtherComprehensiveIncome(sum(list, ComprehensiveIncomeStatementDto::getOtherComprehensiveIncome));
        total.setComprehensiveIncome(sum(list, ComprehensiveIncomeStatementDto::getComprehensiveIncome));

        // 合計行的費用明細（合併所有期間的費用類別）
        total.setExpenseDetails(mergeExpenseDetails(list));

        // ✅ 避免重複加入
        boolean hasTotal = list.stream()
                .anyMatch(dto -> dto.getAccountingPeriod() != null && dto.getAccountingPeriod().startsWith("合計"));
        if (!hasTotal) {
            list.add(total);
        }

        return list;
    }

    @Override
    public List<ComprehensiveIncomeStatementDto> generateComprehensiveIncomeStatement(List<String> periods) {
        log.info("生成多期間綜合損益表比較：periods={}", periods);

        List<ComprehensiveIncomeStatementDto> list = repository.getComprehensiveIncomeStatement(periods);

        if (list == null || list.isEmpty()) {
            return list;
        }

        // 🧮 新增「合計」列（多期間比較的合計）
        ComprehensiveIncomeStatementDto total = new ComprehensiveIncomeStatementDto();
        String label = "合計";

        if (periods != null && !periods.isEmpty()) {
            if (periods.size() == 1) {
                label += String.format(" (%s)", periods.get(0));
            } else {
                String firstPeriod = periods.get(0);
                String lastPeriod = periods.get(periods.size() - 1);
                label += String.format(" (%s ~ %s)", firstPeriod, lastPeriod);
            }
        }

        total.setAccountingPeriod(label);

        // 🔹 累計加總欄位
        total.setRetailSales(sum(list, ComprehensiveIncomeStatementDto::getRetailSales));
        total.setOrderSales(sum(list, ComprehensiveIncomeStatementDto::getOrderSales));
        total.setTotalRevenue(sum(list, ComprehensiveIncomeStatementDto::getTotalRevenue));
        total.setCostOfGoodsSold(sum(list, ComprehensiveIncomeStatementDto::getCostOfGoodsSold));
        total.setGrossProfit(sum(list, ComprehensiveIncomeStatementDto::getGrossProfit));
        total.setTotalOperatingExpenses(sum(list, ComprehensiveIncomeStatementDto::getTotalOperatingExpenses));
        total.setOperatingProfit(sum(list, ComprehensiveIncomeStatementDto::getOperatingProfit));
        total.setOtherIncome(sum(list, ComprehensiveIncomeStatementDto::getOtherIncome));
        total.setOtherExpenses(sum(list, ComprehensiveIncomeStatementDto::getOtherExpenses));
        total.setNetProfit(sum(list, ComprehensiveIncomeStatementDto::getNetProfit));
        total.setOtherComprehensiveIncome(sum(list, ComprehensiveIncomeStatementDto::getOtherComprehensiveIncome));
        total.setComprehensiveIncome(sum(list, ComprehensiveIncomeStatementDto::getComprehensiveIncome));

        // 合計行的費用明細
        total.setExpenseDetails(mergeExpenseDetails(list));

        // ✅ 避免重複加入
        boolean hasTotal = list.stream()
                .anyMatch(dto -> dto.getAccountingPeriod() != null && dto.getAccountingPeriod().startsWith("合計"));
        if (!hasTotal) {
            list.add(total);
        }

        return list;
    }

    /**
     * BigDecimal 累加工具
     */
    private BigDecimal sum(List<ComprehensiveIncomeStatementDto> list,
                          Function<ComprehensiveIncomeStatementDto, BigDecimal> getter) {
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 合併所有期間的費用類別明細（用於合計行）
     */
    private List<ComprehensiveIncomeStatementDto.ExpenseCategoryDetailDto> mergeExpenseDetails(
            List<ComprehensiveIncomeStatementDto> list) {

        return list.stream()
                .filter(dto -> dto.getExpenseDetails() != null)
                .flatMap(dto -> dto.getExpenseDetails().stream())
                .collect(java.util.stream.Collectors.toMap(
                        detail -> detail.getCategoryId(),
                        detail -> detail,
                        (detail1, detail2) -> {
                            // 合併相同類別的費用
                            BigDecimal amount1 = detail1.getAmount() != null ? detail1.getAmount() : BigDecimal.ZERO;
                            BigDecimal amount2 = detail2.getAmount() != null ? detail2.getAmount() : BigDecimal.ZERO;
                            return ComprehensiveIncomeStatementDto.ExpenseCategoryDetailDto.builder()
                                    .categoryId(detail1.getCategoryId())
                                    .categoryName(detail1.getCategoryName())
                                    .accountCode(detail1.getAccountCode())
                                    .isSalary(detail1.getIsSalary())
                                    .amount(amount1.add(amount2))
                                    .build();
                        }
                ))
                .values()
                .stream()
                .sorted((a, b) -> {
                    String codeA = a.getAccountCode() != null ? a.getAccountCode() : "";
                    String codeB = b.getAccountCode() != null ? b.getAccountCode() : "";
                    return codeA.compareTo(codeB);
                })
                .toList();
    }
}

