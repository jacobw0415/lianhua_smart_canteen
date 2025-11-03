package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.report.BalanceSheetReportDto;
import com.lianhua.erp.repository.BalanceSheetReportRepository;
import com.lianhua.erp.service.BalanceSheetReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceSheetReportServiceImpl implements BalanceSheetReportService {

    private final BalanceSheetReportRepository repository;

    @Override
    public List<BalanceSheetReportDto> generateBalanceSheet(String period) {
        return List.of();
    }

    @Override
    public List<BalanceSheetReportDto> generateBalanceSheet(String period, String startDate, String endDate) {

        if (period != null && (startDate == null || endDate == null)) {
            YearMonth ym = YearMonth.parse(period);
            startDate = ym.atDay(1).toString();
            endDate = ym.atEndOfMonth().toString();
        }

        BigDecimal ar = repository.getAccountsReceivable(period);
        BigDecimal ap = repository.getAccountsPayable(period);
        BigDecimal cashReceipts = repository.getCashReceipts(period);
        BigDecimal cashSales = repository.getCashSales(period);

        BigDecimal cash = cashReceipts.add(cashSales);
        BigDecimal totalAssets = ar.add(cash);
        BigDecimal equity = totalAssets.subtract(ap);

        BalanceSheetReportDto dto = BalanceSheetReportDto.builder()
                .accountingPeriod(period)
                .accountsReceivable(ar)
                .accountsPayable(ap)
                .cash(cash)
                .totalAssets(totalAssets)
                .totalLiabilities(ap)
                .equity(equity)
                .build();

        return List.of(dto);
    }
}
