package com.lianhua.erp.service;

import com.lianhua.erp.dto.receipt.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReceiptService {
    ReceiptResponseDto create(ReceiptRequestDto dto);
    ReceiptResponseDto update(Long id, ReceiptRequestDto dto);
    void delete(Long id);
    
    /**
     * React-Admin 收款列表查詢（僅分頁，不含模糊搜尋）
     */
    Page<ReceiptResponseDto> findAll(Pageable pageable);
    
    /**
     * 模糊搜尋 + 分頁查詢收款紀錄（支援 customerName、orderNo、method、accountingPeriod、日期區間）
     */
    Page<ReceiptResponseDto> searchReceipts(ReceiptSearchRequest req, Pageable pageable);
    
    List<ReceiptResponseDto> findByOrderId(Long orderId);
    ReceiptResponseDto findById(Long id);
    
    /**
     * 作廢收款單
     * @param id 收款單 ID
     * @param reason 作廢原因（可選）
     * @return 更新後的收款單 DTO
     */
    ReceiptResponseDto voidReceipt(Long id, String reason);
}
