package com.lianhua.erp.service;

import com.lianhua.erp.dto.purchase.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PurchaseService {
    // ================================================================
    // 🔥 新增：分頁取得所有進貨單（比照 SupplierServiceImpl）
    // ================================================================
    Page<PurchaseResponseDto> getAllPurchases(Pageable pageable);
    PurchaseResponseDto getPurchaseById(Long id);
    PurchaseResponseDto createPurchase(PurchaseRequestDto dto);
    PurchaseResponseDto updatePurchase(Long id, PurchaseRequestDto dto);
    PurchaseResponseDto updateStatus(Long id, String status);
    void deletePurchase(Long id);
    Page<PurchaseResponseDto> searchPurchases(PurchaseSearchRequest req, Pageable pageable);

}