package com.lianhua.erp.service;

import com.lianhua.erp.dto.purchase.PurchaseItemRequestDto;
import com.lianhua.erp.dto.purchase.PurchaseItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PurchaseItemService {
    
    /**
     * 取得指定採購單的所有明細
     */
    List<PurchaseItemDto> findByPurchaseId(Long purchaseId);
    
    /**
     * 分頁取得所有採購明細清單（不分採購單）
     */
    Page<PurchaseItemDto> findAllPaged(Pageable pageable);
    
    /**
     * 為指定採購單新增一筆明細
     */
    PurchaseItemDto create(Long purchaseId, PurchaseItemRequestDto dto);
    
    /**
     * 更新指定的採購明細
     */
    PurchaseItemDto update(Long purchaseId, Long itemId, PurchaseItemRequestDto dto);
    
    /**
     * 刪除指定的採購明細
     */
    void delete(Long purchaseId, Long itemId);
}

