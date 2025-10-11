package com.lianhua.erp.service;

import com.lianhua.erp.dto.purchase.*;
import java.util.List;

public interface PurchaseService {
    List<PurchaseDto> getAllPurchases();
    PurchaseDto getPurchaseById(Long id);
    PurchaseDto createPurchase(PurchaseRequestDto dto);
    PurchaseDto updatePurchase(Long id, PurchaseRequestDto dto);
    void deletePurchase(Long id);
}
