package com.lianhua.erp.service;

import com.lianhua.erp.dto.payment.PaymentResponseDto;
import com.lianhua.erp.dto.payment.PaymentSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface PaymentService {

    /**
     * React-Admin 付款列表查詢（僅分頁，不含模糊搜尋）
     */
    Page<PaymentResponseDto> findAll(Pageable pageable);

    /**
     * 模糊搜尋 + 分頁查詢付款紀錄（支援 supplierName、item、method、accountingPeriod、日期區間）
     */
    Page<PaymentResponseDto> searchPayments(PaymentSearchRequest req, Pageable pageable);

    /**
     * 依進貨單 ID 查詢其付款紀錄（常用於 PurchaseEdit Drawer）
     */
    PaymentResponseDto findByPurchaseId(Long purchaseId);

    /**
     * 刪除某進貨單下的所有付款紀錄
     */
    void deleteByPurchaseId(Long purchaseId);
}