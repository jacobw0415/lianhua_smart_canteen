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
import java.util.Objects;
import java.util.function.Function;

/**
 * 💸 應付帳齡報表服務實作
 * 支援供應商 ID、最小逾期天數、會計期間等多條件查詢。
 * 並自動加上「合計」列（僅金額欄位）。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class APAgingReportServiceImpl implements APAgingReportService {

    private final APAgingReportRepository repository;

    @Override
    public List<APAgingReportDto> getAgingPayables(Long supplierId, Integer minOverdue, String period) {
        // 📊 查詢資料
        List<APAgingReportDto> list = repository.findAgingPayables(supplierId, minOverdue, period);
        if (list.isEmpty()) return list;

        // 🧮 建立合計列
        APAgingReportDto total = new APAgingReportDto();
        total.setSupplierName(buildLabel(supplierId, minOverdue, period));
        total.setTotalAmount(sum(list, APAgingReportDto::getTotalAmount));
        total.setPaidAmount(sum(list, APAgingReportDto::getPaidAmount));
        total.setBalance(sum(list, APAgingReportDto::getBalance));

        //  將合計列放在最下方
        list.add(total);
        return list;
    }

    /** 🔹 BigDecimal 安全累加 */
    private BigDecimal sum(List<APAgingReportDto> list, Function<APAgingReportDto, BigDecimal> getter) {
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 🔹 動態標籤生成 */
    private String buildLabel(Long supplierId, Integer minOverdue, String period) {
        StringBuilder label = new StringBuilder("合計");
        if (supplierId != null) label.append(String.format("（供應商ID: %d）", supplierId));
        if (minOverdue != null) label.append(String.format("（逾期 ≥ %d 天）", minOverdue));
        if (period != null && !period.isBlank()) label.append(String.format("（期間: %s）", period));
        return label.toString();
    }
}
