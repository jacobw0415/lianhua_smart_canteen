package com.lianhua.erp.service;

import com.lianhua.erp.dto.ar.ARAgingFilterDto;
import com.lianhua.erp.dto.ar.ARAgingOrderDetailDto;
import com.lianhua.erp.dto.ar.ARAgingSummaryDto;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * ⭐ 應收帳齡（AR Aging）Service 介面
 *
 * 設計原則：
 * - Summary 查詢支援 Filter + Pageable
 * - 匯出 / 報表使用不分頁版本
 * - Detail 查詢維持單一客戶未收款明細
 */
public interface ARAgingService {

    // ======================================================
    // 🔥 第 1 層：Summary（分頁 + 搜尋 → UI 使用）
    // ======================================================
    Page<ARAgingSummaryDto> getAgingSummary(
            ARAgingFilterDto filter,
            Pageable pageable
    );

    // ======================================================
    // 🔥 第 1 層（不分頁）給匯出 / 報表專用
    // ======================================================
    List<ARAgingSummaryDto> getAgingSummaryAll();

    // ======================================================
    // 🔥 匯出（含篩選、支援 page/all）
    // ======================================================
    ExportPayload exportAgingSummary(
            ARAgingFilterDto filter,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    );

    // ======================================================
    // 🔥 第 2 層：取得某客戶逐筆應收明細
    // ======================================================
    List<ARAgingOrderDetailDto> getCustomerOrders(Long customerId);
}

