package com.lianhua.erp.service.impl;


import com.lianhua.erp.dto.report.CashFlowReportDto;
import com.lianhua.erp.repository.CashFlowReportRepository;
import com.lianhua.erp.service.CashFlowReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 現金流量報表服務實作
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CashFlowReportServiceImpl implements CashFlowReportService {

    private final CashFlowReportRepository repository;

    @Override
    public List<CashFlowReportDto> getCashFlowReport(String startDate, String endDate, String method, String period) {
        List<Object[]> results = repository.findMonthlyCashFlowReport(startDate, endDate, method, period);

        return results.stream().map(r -> CashFlowReportDto.builder()
                .accountingPeriod((String) r[0])
                .totalSales((BigDecimal) r[1])
                .totalReceipts((BigDecimal) r[2])
                .totalPayments((BigDecimal) r[3])
                .totalExpenses((BigDecimal) r[4])
                .totalInflow((BigDecimal) r[5])
                .totalOutflow((BigDecimal) r[6])
                .netCashFlow((BigDecimal) r[7])
                .build()
        ).toList();
    }
}