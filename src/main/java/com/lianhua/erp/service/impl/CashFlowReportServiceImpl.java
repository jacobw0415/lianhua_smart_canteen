package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.report.CashFlowReportDto;
import com.lianhua.erp.repository.CashFlowReportRepository;
import com.lianhua.erp.service.CashFlowReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashFlowReportServiceImpl implements CashFlowReportService {

    private final CashFlowReportRepository repository;

    @Override
    public List<CashFlowReportDto> generateCashFlow(String period, String startDate, String endDate) {

        List<CashFlowReportDto> list = repository.getCashFlow(period, startDate, endDate);

        if (list == null || list.isEmpty()) {
            return list;
        }

        // ✅ 生成合計 DTO
        CashFlowReportDto total = new CashFlowReportDto();

        // 📅 自動標籤邏輯
        String label = "合計";
        if (startDate != null && endDate != null) {
            label += String.format(" (%s ~ %s)", startDate, endDate);
        } else if (period != null && !period.isBlank()) {
            label += String.format(" (%s)", period);
        }
        total.setAccountingPeriod(label);

        // 💰 四大金額加總
        total.setTotalSales(sum(list, CashFlowReportDto::getTotalSales));
        total.setTotalReceipts(sum(list, CashFlowReportDto::getTotalReceipts));
        total.setTotalPayments(sum(list, CashFlowReportDto::getTotalPayments));
        total.setTotalExpenses(sum(list, CashFlowReportDto::getTotalExpenses));

        // 💵 自動加總流入、流出與淨現金
        total.setTotalInflow(total.getTotalSales().add(total.getTotalReceipts()));
        total.setTotalOutflow(total.getTotalPayments().add(total.getTotalExpenses()));
        total.setNetCashFlow(total.getTotalInflow().subtract(total.getTotalOutflow()));

        // ✅ 避免重複加入
        boolean hasTotal = list.stream()
                .anyMatch(dto -> dto.getAccountingPeriod() != null && dto.getAccountingPeriod().startsWith("合計"));
        if (!hasTotal) {
            list.add(total);
        }

        return list;
    }

    /**
     * 🧩 共用加總函式
     */
    private BigDecimal sum(List<CashFlowReportDto> list, java.util.function.Function<CashFlowReportDto, BigDecimal> getter) {
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}