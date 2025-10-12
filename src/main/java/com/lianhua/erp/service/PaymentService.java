package com.lianhua.erp.service;

import com.lianhua.erp.dto.payment.PaymentResponseDto;
import java.util.List;

public interface PaymentService {
    List<PaymentResponseDto> findAll();
    PaymentResponseDto findByPurchaseId(Long purchaseId);
    void deleteByPurchaseId(Long purchaseId);
}
