package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.report.ARSummaryReportDto;
import com.lianhua.erp.repository.ARSummaryReportRepository;
import com.lianhua.erp.service.ARSummaryReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ARSummaryReportServiceImpl implements ARSummaryReportService {

    private final ARSummaryReportRepository repository;

    @Override
    public List<ARSummaryReportDto> generateSummary(String period) {
        return repository.getSummaryList(period, null);
    }

    @Override
    public List<ARSummaryReportDto> generateSummary(String period, String endDate) {
        return repository.getSummaryList(period, endDate);
    }

    @Override
    public List<ARSummaryReportDto> generateSummary(List<String> periods) {
        // Aggregate summaries per period using the existing 2-arg repository method
        List<ARSummaryReportDto> list = new java.util.ArrayList<>();
        for (String p : periods) {
            list.addAll(repository.getSummaryList(p, null));
        }

        if (list == null || list.isEmpty()) {
            return list;
        }

        // 加上合計列，與其他報表一致
        ARSummaryReportDto total = new ARSummaryReportDto();
        total.setAccountingPeriod("合計");

        BigDecimal totalReceivable = sum(list, ARSummaryReportDto::getTotalReceivable);
        BigDecimal totalReceived = sum(list, ARSummaryReportDto::getTotalReceived);
        BigDecimal totalOutstanding = sum(list, ARSummaryReportDto::getTotalOutstanding);

        total.setTotalReceivable(totalReceivable);
        total.setTotalReceived(totalReceived);
        total.setTotalOutstanding(totalOutstanding);

        boolean hasTotal = list.stream()
                .anyMatch(dto -> dto.getAccountingPeriod() != null && dto.getAccountingPeriod().startsWith("合計"));
        if (!hasTotal) {
            list.add(total);
        }

        return list;
    }

    private BigDecimal sum(List<ARSummaryReportDto> list, java.util.function.Function<ARSummaryReportDto, BigDecimal> extractor) {
        return list.stream()
                .map(extractor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
