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
    
    /**
     * 作廢進貨單
     * 
     * 業務邏輯：
     * 1. 檢查進貨單是否存在且未作廢
     * 2. 自動作廢所有相關的有效付款單
     * 3. 標記進貨單為已作廢
     * 4. 更新進貨單狀態、作廢時間、作廢原因
     * 
     * @param id 進貨單 ID
     * @param reason 作廢原因
     * @return 作廢後的進貨單 DTO
     */
    PurchaseResponseDto voidPurchase(Long id, String reason);

}