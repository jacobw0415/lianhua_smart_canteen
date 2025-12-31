package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.ar.ARAgingFilterDto;
import com.lianhua.erp.dto.ar.ARAgingOrderDetailDto;
import com.lianhua.erp.dto.ar.ARAgingSummaryDto;
import com.lianhua.erp.repository.ARAgingRepository;
import com.lianhua.erp.service.ARAgingService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ARAgingServiceImpl implements ARAgingService {

    private final ARAgingRepository arAgingRepository;

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
    // 🔥 Detail（單一客戶逐筆未收款訂單）
    // =============================================================
    @Override
    public List<ARAgingOrderDetailDto> getCustomerOrders(Long customerId) {
        return arAgingRepository.findOrdersByCustomerId(customerId);
    }
}

