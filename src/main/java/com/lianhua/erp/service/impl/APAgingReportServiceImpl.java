package com.lianhua.erp.service.impl;


import com.lianhua.erp.dto.report.APAgingReportDto;
import com.lianhua.erp.repository.APAgingReportRepository;
import com.lianhua.erp.service.APAgingReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 應付帳齡報表服務實作
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class APAgingReportServiceImpl implements APAgingReportService {

    private final APAgingReportRepository repository;

    @Override
    public List<APAgingReportDto> getAgingPayables(Long supplierId, Integer minOverdue, String period) {
        List<Object[]> results = repository.findAgingPayables(supplierId, minOverdue, period);

        return results.stream().map(r -> APAgingReportDto.builder()
                .supplierName((String) r[0])
                .purchaseId(((Number) r[1]).longValue())
                .purchaseDate((String) r[2])
                .totalAmount((BigDecimal) r[3])
                .paidAmount((BigDecimal) r[4])
                .balance((BigDecimal) r[5])
                .daysOverdue(((Number) r[6]).intValue())
                .agingBucket((String) r[7])
                .build()
        ).toList();
    }
}
