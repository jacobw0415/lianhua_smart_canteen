package com.lianhua.erp.service;

import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface OrderService {


    Page<OrderResponseDto> page(Pageable pageable);

    // ================================
    // 查詢訂單（搜尋 + 分頁）
    // ================================
    Page<OrderResponseDto> search(
            OrderSearchRequest searchRequest,
            Pageable pageable
    );

    /**
     * 匯出訂單列表（篩選條件與 {@link #search} 相同；scope=all 時不分頁）。
     */
    ExportPayload exportOrders(
            OrderSearchRequest searchRequest,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    );

    // ================================
    // 查詢單筆訂單
    // ================================
    OrderResponseDto findById(Long id);

    // ================================
    // 建立訂單
    // ================================
    OrderResponseDto create(OrderRequestDto dto);

    // ================================
    // 更新訂單
    // ================================
    OrderResponseDto update(Long id, OrderRequestDto dto);

    // ================================
    // 刪除訂單
    // ================================
    void delete(Long id);

    void voidOrder(String orderNo, String voidReason);

}
