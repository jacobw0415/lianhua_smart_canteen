package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.ap.APAgingFilterDto;
import com.lianhua.erp.dto.ap.APAgingSummaryDto;
import com.lianhua.erp.dto.ap.APAgingPurchaseDetailDto;
import com.lianhua.erp.repository.APAgingRepository;
import com.lianhua.erp.service.APAgingService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class APAgingServiceImpl implements APAgingService {

    private final APAgingRepository apAgingRepository;

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
    // 🔥 Detail（單一供應商逐筆未付款進貨）
    // =============================================================
    @Override
    public List<APAgingPurchaseDetailDto> getSupplierPurchases(Long supplierId) {
        return apAgingRepository.findPurchasesBySupplierId(supplierId);
    }
}