package com.lianhua.erp.service;

import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.sale.*;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SalesService {

    // ============================================================
    // 建立 / 更新
    // ============================================================

    SalesResponseDto create(SalesRequestDto dto);

    SalesResponseDto update(Long id, SalesRequestDto dto);

    // ============================================================
    // 🔥 分頁查詢（取代舊 list）
    // ============================================================

    /**
     * 分頁取得所有銷售紀錄（不含搜尋條件）
     * 對齊 PurchaseController 的 getAllPurchases(Pageable)
     */
    Page<SalesResponseDto> getAllSales(Pageable pageable);

    /**
     * 分頁搜尋銷售紀錄（含條件）
     */
    Page<SalesResponseDto> search(
            SaleSearchRequestDto req,
            Pageable pageable
    );

    /**
     * 匯出銷售列表（篩選條件與 {@link #search} 相同；scope=all 時不分頁）。
     */
    ExportPayload exportSales(
            SaleSearchRequestDto req,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    );

    // ============================================================
    // 其他操作
    // ============================================================

    void delete(Long id);

    SalesResponseDto findById(Long id);

    List<SalesResponseDto> findByProduct(Long productId);
}
