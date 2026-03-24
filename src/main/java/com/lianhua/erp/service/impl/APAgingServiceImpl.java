package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.ap.APAgingFilterDto;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.ap.APAgingSummaryDto;
import com.lianhua.erp.dto.ap.APAgingPurchaseDetailDto;
import com.lianhua.erp.repository.APAgingRepository;
import com.lianhua.erp.export.ExportFilenameUtils;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import com.lianhua.erp.export.TabularExporter;
import com.lianhua.erp.service.APAgingService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class APAgingServiceImpl implements APAgingService {

    private final APAgingRepository apAgingRepository;

    private static final String[] AP_AGING_EXPORT_HEADERS = new String[]{
            "供應商名稱",
            "0-30 天",
            "31-60 天",
            "60 天以上",
            "應付總額",
            "已付款總額",
            "未付款總額"
    };

    @Value("${app.export.max-rows:50000}")
    private int maxExportRows;

    // =============================================================
    // 🔥 Summary（不分頁 → 匯出 / 報表）
    // =============================================================
    @Override
    public List<APAgingSummaryDto> getAgingSummaryAll() {
        return apAgingRepository.findAgingSummary();
    }

    // =============================================================
    // 🔥 Summary（分頁 + 搜尋 → UI 使用）
    // =============================================================
    @Override
    public Page<APAgingSummaryDto> getAgingSummary(
            APAgingFilterDto filter,
            Pageable pageable
    ) {
        return apAgingRepository.findAgingSummaryPaged(
                filter,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
    }

    // =============================================================
    // ✅ 匯出 AP Aging Summary（含篩選、支援 page/all）
    // =============================================================
    @Override
    @Transactional(readOnly = true)
    public ExportPayload exportAgingSummary(
            APAgingFilterDto filter,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    ) {
        APAgingFilterDto req = filter == null ? new APAgingFilterDto() : filter;

        ExportFormat safeFormat = format == null ? ExportFormat.XLSX : format;
        ExportScope safeScope = scope == null ? ExportScope.ALL : scope;

        int pageSize = pageable == null || pageable.getPageSize() <= 0 ? 20 : pageable.getPageSize();

        List<String[]> rows = new ArrayList<>();
        if (safeScope == ExportScope.ALL) {
            int step = Math.min(Math.max(pageSize, 50), 2000);

            Page<APAgingSummaryDto> first =
                    getAgingSummary(req, PageRequest.of(0, step));
            long total = first.getTotalElements();
            if (total > maxExportRows) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "匯出筆數超過上限 (" + maxExportRows + ")，請縮小篩選條件");
            }

            for (APAgingSummaryDto item : first.getContent()) {
                rows.add(toAgingExportRow(item));
            }

            for (int p = 1; p < first.getTotalPages(); p++) {
                Page<APAgingSummaryDto> page =
                        getAgingSummary(req, PageRequest.of(p, step));
                for (APAgingSummaryDto item : page.getContent()) {
                    rows.add(toAgingExportRow(item));
                }
            }
        } else {
            int safePageSize = pageSize > 200 ? 200 : pageSize;
            Pageable p = pageable == null
                    ? PageRequest.of(0, safePageSize)
                    : PageRequest.of(
                            Math.max(pageable.getPageNumber(), 0),
                            pageable.getPageSize() <= 0 || pageable.getPageSize() > 200 ? safePageSize : pageable.getPageSize(),
                            pageable.getSort()
                    );
            for (APAgingSummaryDto item : getAgingSummary(req, p).getContent()) {
                rows.add(toAgingExportRow(item));
            }
        }

        byte[] data = switch (safeFormat) {
            case XLSX -> TabularExporter.toXlsx("ap_aging", AP_AGING_EXPORT_HEADERS, rows);
            case CSV -> TabularExporter.toCsvUtf8Bom(AP_AGING_EXPORT_HEADERS, rows);
        };

        String filename = ExportFilenameUtils.build("ap_aging", safeFormat);
        return new ExportPayload(data, filename, safeFormat.mediaType());
    }

    private static String[] toAgingExportRow(APAgingSummaryDto a) {
        return new String[]{
                nz(a.getSupplierName()),
                toPlain(a.getAging0to30()),
                toPlain(a.getAging31to60()),
                toPlain(a.getAging60plus()),
                toPlain(a.getTotalAmount()),
                toPlain(a.getPaidAmount()),
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
    // 🔥 Detail（單一供應商逐筆未付款進貨）
    // =============================================================
    @Override
    public List<APAgingPurchaseDetailDto> getSupplierPurchases(Long supplierId) {
        return apAgingRepository.findPurchasesBySupplierId(supplierId);
    }
}