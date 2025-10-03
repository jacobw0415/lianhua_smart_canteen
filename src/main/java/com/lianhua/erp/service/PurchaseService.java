package com.lianhua.erp.service;

import com.lianhua.erp.dto.PurchaseDto;
import com.lianhua.erp.dto.PaymentDto;
import java.util.List;

public interface PurchaseService {
    List<PurchaseDto> getAllPurchases();
    PurchaseDto getPurchaseById(Long id);
    PurchaseDto createPurchase(PurchaseDto dto);
    PurchaseDto updatePurchase(Long id, PurchaseDto dto);
    void deletePurchase(Long id);
}
