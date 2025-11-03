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

/**
 * 報表服務實作
 * 統一封裝損益報表的查詢邏輯。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository repository;

    @Override
    public List<ProfitReportDto> getMonthlyProfitReport(String period, String startDate, String endDate) {
        List<Object[]> results = repository.findMonthlyProfitReport(period, startDate, endDate);

        return results.stream().map(r -> ProfitReportDto.builder()
                .accountingPeriod((String) r[0])
                .totalSales((BigDecimal) r[1])
                .totalOrders((BigDecimal) r[2])
                .totalRevenue((BigDecimal) r[3])
                .totalPurchase((BigDecimal) r[4])
                .totalExpense((BigDecimal) r[5])
                .netProfit((BigDecimal) r[6])
                .build()
        ).toList();
    }
}
