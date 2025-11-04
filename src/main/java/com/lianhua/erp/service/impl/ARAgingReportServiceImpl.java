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
import java.util.Objects;
import java.util.function.Function;

/**
 * 💰 應收帳齡報表服務實作
 * 支援客戶 ID、最小逾期天數、會計期間等多條件查詢。
 * 自動加上「合計」列（僅金額欄位）。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ARAgingReportServiceImpl implements ARAgingReportService {

    private final ARAgingReportRepository repository;

    @Override
    public List<ARAgingReportDto> getAgingReceivables(Long customerId, Integer minOverdue, String period) {
        // 📊 查詢資料
        List<ARAgingReportDto> list = repository.findAgingReceivables(customerId, minOverdue, period);
        if (list.isEmpty()) return list;

        // 🧮 建立合計列
        ARAgingReportDto total = new ARAgingReportDto();
        total.setCustomerName(buildLabel(customerId, minOverdue, period));
        total.setTotalAmount(sum(list, ARAgingReportDto::getTotalAmount));
        total.setReceivedAmount(sum(list, ARAgingReportDto::getReceivedAmount));
        total.setBalance(sum(list, ARAgingReportDto::getBalance));

        //  合計列放最下方
        list.add(total);
        return list;
    }

    /** 🔹 BigDecimal 安全累加 */
    private BigDecimal sum(List<ARAgingReportDto> list, Function<ARAgingReportDto, BigDecimal> getter) {
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 🔹 動態標籤生成 */
    private String buildLabel(Long customerId, Integer minOverdue, String period) {
        StringBuilder label = new StringBuilder("合計");
        if (customerId != null) label.append(String.format("（客戶ID: %d）", customerId));
        if (minOverdue != null) label.append(String.format("（逾期 ≥ %d 天）", minOverdue));
        if (period != null && !period.isBlank()) label.append(String.format("（期間: %s）", period));
        return label.toString();
    }
}
