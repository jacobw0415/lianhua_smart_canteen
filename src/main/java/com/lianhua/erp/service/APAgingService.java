package com.lianhua.erp.service;

import com.lianhua.erp.dto.ap.APAgingPurchaseDetailDto;
import com.lianhua.erp.dto.ap.APAgingSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * ⭐ 應付帳齡（AP Aging）ERP 標準三層模型 Service 介面
 * 規格比照 PurchaseService，採用 Pageable / Page<T> 分頁標準。
 */
public interface APAgingService {

    // ======================================================
    // 🔥 第 1 層：Summary（分頁取得供應商帳齡彙總）
    // ======================================================
    Page<APAgingSummaryDto> getAgingSummary(Pageable pageable);

    // ======================================================
    // 🔥 第 1 層（不分頁）給匯出專用
    // ======================================================
    List<APAgingSummaryDto> getAgingSummaryAll();

    // ======================================================
    // 🔥 第 2 層：取得某供應商逐筆應付明細（不分頁）
    // ======================================================
    List<APAgingPurchaseDetailDto> getSupplierPurchases(Long supplierId);

}