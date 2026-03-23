package com.lianhua.erp.service;

import com.lianhua.erp.dto.ap.APAgingFilterDto;
import com.lianhua.erp.dto.ap.APAgingPurchaseDetailDto;
import com.lianhua.erp.dto.ap.APAgingSummaryDto;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * ⭐ 應付帳齡（AP Aging）Service 介面
 *
 * 設計原則：
 * - Summary 查詢支援 Filter + Pageable
 * - 匯出 / 報表使用不分頁版本
 * - Detail 查詢維持單一供應商未付款明細
 */
public interface APAgingService {

    // ======================================================
    // 🔥 第 1 層：Summary（分頁 + 搜尋 → UI 使用）
    // ======================================================
    Page<APAgingSummaryDto> getAgingSummary(
            APAgingFilterDto filter,
            Pageable pageable
    );

    // ======================================================
    // 🔥 第 1 層（不分頁）給匯出 / 報表專用
    // ======================================================
    List<APAgingSummaryDto> getAgingSummaryAll();

    // ======================================================
    // 🔥 匯出（含篩選、支援 page/all）
    // ======================================================
    ExportPayload exportAgingSummary(
            APAgingFilterDto filter,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    );

    // ======================================================
    // 🔥 第 2 層：取得某供應商逐筆應付明細
    // ======================================================
    List<APAgingPurchaseDetailDto> getSupplierPurchases(Long supplierId);
}