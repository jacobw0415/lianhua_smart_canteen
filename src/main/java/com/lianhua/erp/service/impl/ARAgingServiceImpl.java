package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.ar.ARAgingFilterDto;
import com.lianhua.erp.dto.ar.ARAgingOrderDetailDto;
import com.lianhua.erp.dto.ar.ARAgingSummaryDto;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.repository.ARAgingRepository;
import com.lianhua.erp.export.ExportFilenameUtils;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import com.lianhua.erp.export.TabularExporter;
import com.lianhua.erp.service.ARAgingService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ARAgingServiceImpl implements ARAgingService {

    private final ARAgingRepository arAgingRepository;

    private static final String[] AGING_EXPORT_HEADERS = new String[]{
            "客戶名稱",
            "0-30 天",
            "31-60 天",
            "60 天以上",
            "應收總額",
            "已收款總額",
            "未收款總額"
    };

    @Value("${app.export.max-rows:50000}")
    private int maxExportRows;

    // =============================================================
    // 🔥 Summary（不分頁 → 匯出 / 報表）
    // =============================================================
    @Override
    public List<ARAgingSummaryDto> getAgingSummaryAll() {
        return arAgingRepository.findAgingSummary();
    }

    // =============================================================
    // 🔥 Summary（分頁 + 搜尋 → UI 使用）
    // =============================================================
    @Override
    public Page<ARAgingSummaryDto> getAgingSummary(
            ARAgingFilterDto filter,
            Pageable pageable
    ) {
        return arAgingRepository.findAgingSummaryPaged(
                filter,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
    }

    // =============================================================
    // ✅ 收款狀態：匯出（含篩選、支援 page/all）
    // =============================================================
    @Override
    @Transactional(readOnly = true)
    public ExportPayload exportAgingSummary(
            ARAgingFilterDto filter,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    ) {
        ARAgingFilterDto req = filter == null ? new ARAgingFilterDto() : filter;
        ExportFormat safeFormat = format == null ? ExportFormat.XLSX : format;
        ExportScope safeScope = scope == null ? ExportScope.ALL : scope;

        int pageSize = pageable == null || pageable.getPageSize() <= 0 ? 20 : pageable.getPageSize();

        List<String[]> rows = new ArrayList<>();
        if (safeScope == ExportScope.ALL) {
            int step = Math.min(Math.max(pageSize, 50), 2000);

            Page<ARAgingSummaryDto> first =
                    getAgingSummary(req, PageRequest.of(0, step));
            long total = first.getTotalElements();
            if (total > maxExportRows) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "匯出筆數超過上限 (" + maxExportRows + ")，請縮小篩選條件");
            }

            for (ARAgingSummaryDto item : first.getContent()) {
                rows.add(toAgingExportRow(item));
            }

            for (int p = 1; p < first.getTotalPages(); p++) {
                Page<ARAgingSummaryDto> page =
                        getAgingSummary(req, PageRequest.of(p, step));
                for (ARAgingSummaryDto item : page.getContent()) {
                    rows.add(toAgingExportRow(item));
                }
            }
        } else {
            int safePageSize = pageSize > 200 ? 200 : pageSize;
            Pageable p = pageable == null ? PageRequest.of(0, safePageSize) : PageRequest.of(
                    Math.max(pageable.getPageNumber(), 0),
                    pageable.getPageSize() <= 0 || pageable.getPageSize() > 200 ? safePageSize : pageable.getPageSize(),
                    pageable.getSort()
            );
            for (ARAgingSummaryDto item : getAgingSummary(req, p).getContent()) {
                rows.add(toAgingExportRow(item));
            }
        }

        byte[] data = switch (safeFormat) {
            case XLSX -> TabularExporter.toXlsx("ar_aging", AGING_EXPORT_HEADERS, rows);
            case CSV -> TabularExporter.toCsvUtf8Bom(AGING_EXPORT_HEADERS, rows);
        };

        String filename = ExportFilenameUtils.build("ar_aging", safeFormat);
        return new ExportPayload(data, filename, safeFormat.mediaType());
    }

    private static String[] toAgingExportRow(ARAgingSummaryDto a) {
        return new String[]{
                nz(a.getCustomerName()),
                toPlain(a.getAging0to30()),
                toPlain(a.getAging31to60()),
                toPlain(a.getAging60plus()),
                toPlain(a.getTotalAmount()),
                toPlain(a.getReceivedAmount()),
                toPlain(a.getBalance())
        };
    }

    private static String toPlain(BigDecimal v) {
        return v == null ? "" : v.toPlainString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    // =============================================================
    // 🔥 Detail（單一客戶逐筆未收款訂單）
    // =============================================================
    @Override
    public List<ARAgingOrderDetailDto> getCustomerOrders(Long customerId) {
        return arAgingRepository.findOrdersByCustomerId(customerId);
    }
}

