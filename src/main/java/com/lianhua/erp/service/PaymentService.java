package com.lianhua.erp.service;

import com.lianhua.erp.dto.payment.PaymentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    /**
     * React-Admin 付款列表查詢（支援分頁）
     */
    Page<PaymentResponseDto> findAll(Pageable pageable);

    /**
     * 依進貨單 ID 查詢其付款紀錄（常用於 PurchaseEdit）
     */
    PaymentResponseDto findByPurchaseId(Long purchaseId);

    /**
     * 刪除某進貨單下的所有付款紀錄
     */
    void deleteByPurchaseId(Long purchaseId);
}