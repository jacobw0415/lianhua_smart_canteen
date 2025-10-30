package com.lianhua.erp.service;

import com.lianhua.erp.dto.receipt.*;
import java.util.List;

public interface ReceiptService {
    ReceiptResponseDto create(ReceiptRequestDto dto);
    ReceiptResponseDto update(Long id, ReceiptRequestDto dto);
    void delete(Long id);
    List<ReceiptResponseDto> findAll();
    List<ReceiptResponseDto> findByOrderId(Long orderId);
    ReceiptResponseDto findById(Long id);
}
