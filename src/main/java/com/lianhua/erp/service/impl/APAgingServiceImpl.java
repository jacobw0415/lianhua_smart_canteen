package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.ap.APAgingSummaryDto;
import com.lianhua.erp.dto.ap.APAgingPurchaseDetailDto;
import com.lianhua.erp.repository.APAgingRepository;
import com.lianhua.erp.service.APAgingService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class APAgingServiceImpl implements APAgingService {

    private final APAgingRepository apAgingRepository;

    // =============================================================
    // 🔥 第 1 層 Summary（不分頁 → 匯出 / 報表）
    // =============================================================
    @Override
    public List<APAgingSummaryDto> getAgingSummaryAll() {
        return apAgingRepository.findAgingSummary();
    }

    // =============================================================
    // 🔥 第 1 層 Summary（React-Admin 分頁版）
    // =============================================================
    @Override
    public Page<APAgingSummaryDto> getAgingSummary(Pageable pageable) {

        // 取得所有紀錄（由 Repository 整批查出）
        List<APAgingSummaryDto> all = apAgingRepository.findAgingSummary();
        int total = all.size();

        // React-Admin 需要 page=0 開始，因此使用 pageable.getOffset()
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);

        // 避免 page > total 時出錯
        List<APAgingSummaryDto> content =
                (start >= total) ? List.of() : all.subList(start, end);

        return new PageImpl<>(content, pageable, total);
    }

    // =============================================================
    // 🔥 第 2 層 Detail（單一供應商逐筆未付款進貨）
    // =============================================================
    @Override
    public List<APAgingPurchaseDetailDto> getSupplierPurchases(Long supplierId) {
        return apAgingRepository.findPurchasesBySupplierId(supplierId);
    }
}