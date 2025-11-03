package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.report.ARAgingReportDto;
import com.lianhua.erp.repository.ARAgingReportRepository;
import com.lianhua.erp.service.ARAgingReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 應收帳齡報表服務實作
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ARAgingReportServiceImpl implements ARAgingReportService {

    private final ARAgingReportRepository repository;

    @Override
    public List<ARAgingReportDto> getAgingReceivables(Long customerId, Integer minOverdue, String period) {
        List<Object[]> results = repository.findAgingReceivables(customerId, minOverdue, period);

        return results.stream().map(r -> ARAgingReportDto.builder()
                .customerName((String) r[0])
                .orderId(((Number) r[1]).longValue())
                .orderDate((String) r[2])
                .deliveryDate((String) r[3])
                .totalAmount((BigDecimal) r[4])
                .receivedAmount((BigDecimal) r[5])
                .balance((BigDecimal) r[6])
                .daysOverdue(((Number) r[7]).intValue())
                .agingBucket((String) r[8])
                .build()
        ).toList();
    }
}
