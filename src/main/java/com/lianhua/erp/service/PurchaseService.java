package com.lianhua.erp.service;

import com.lianhua.erp.dto.purchase.*;
import java.util.List;

public interface PurchaseService {
    List<PurchaseResponseDto> getAllPurchases();
    PurchaseResponseDto getPurchaseById(Long id);
    PurchaseResponseDto createPurchase(PurchaseRequestDto dto);
    PurchaseResponseDto updatePurchase(Long id, PurchaseRequestDto dto);
    PurchaseResponseDto updateStatus(Long id, String status);
    void deletePurchase(Long id);
    List<PurchaseResponseDto> findAll();
}