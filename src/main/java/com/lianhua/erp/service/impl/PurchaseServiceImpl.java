package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.*;
import com.lianhua.erp.dto.purchase.PurchaseDto;
import com.lianhua.erp.dto.purchase.PurchaseRequestDto;
import com.lianhua.erp.mapper.PurchaseMapper;
import com.lianhua.erp.mapper.PaymentMapper;
import com.lianhua.erp.repository.PurchaseRepository;
import com.lianhua.erp.repository.SupplierRepository;
import com.lianhua.erp.service.PurchaseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {
    
    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseMapper purchaseMapper;
    private final PaymentMapper paymentMapper;
    
    /**
     * 取得所有進貨單
     */
    @Override
    public java.util.List<PurchaseDto> getAllPurchases() {
        return purchaseRepository.findAll()
                .stream()
                .map(purchaseMapper::toDto)
                .toList();
    }
    
    /**
     * 查詢單筆進貨單
     */
    @Override
    public PurchaseDto getPurchaseById(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的進貨單 (ID: " + id + ")"));
        return purchaseMapper.toDto(purchase);
    }
    
    /**
     * 建立進貨單（含付款）
     */
    @Override
    @Transactional
    public PurchaseDto createPurchase(PurchaseRequestDto dto) {
        try {
            // 1️⃣ 驗證供應商存在
            Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到供應商 ID：" + dto.getSupplierId()));
            
            // 2️⃣ 建立 Purchase 實體並關聯 Supplier
            Purchase purchase = purchaseMapper.toEntity(dto);
            purchase.setSupplier(supplier);
            
            // 3️⃣ 若有付款資料，轉換並設定回 purchase
            if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {
                Set<Payment> payments = dto.getPayments().stream()
                        .map(paymentMapper::toEntity)
                        .peek(p -> {
                            p.setId(null);          // ✅ 關鍵 1：強制清空 ID，確保為新實體
                            p.setPurchase(purchase); // ✅ 關鍵 2：設定父層
                        })
                        .collect(Collectors.toSet());
                purchase.setPayments(payments);
            }
            
            // 4️⃣ 儲存（CascadeType.ALL 自動儲存付款資料）
            Purchase saved = purchaseRepository.save(purchase);
            
            // 5️⃣ 自動更新狀態（PENDING / PARTIAL / PAID）
            updatePurchaseStatus(saved);
            
            // 6️⃣ 回傳
            return purchaseMapper.toDto(saved);
            
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("資料衝突，可能為重複的進貨紀錄或供應商錯誤。", e);
        }
    }
    
    /**
     * 更新進貨單（含付款更新）
     */
    @Override
    @Transactional
    public PurchaseDto updatePurchase(Long id, PurchaseRequestDto dto) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 (ID: " + id + ")"));
        
        // 1️⃣ 更新主表欄位
        purchase.setPurchaseDate(dto.getPurchaseDate());
        purchase.setItem(dto.getItem());
        purchase.setQty(dto.getQty());
        purchase.setUnitPrice(dto.getUnitPrice());
        purchase.setTax(dto.getTax());
        purchase.setStatus(Purchase.Status.valueOf(dto.getStatus().toUpperCase()));
        
        // 2️⃣ 若有付款更新 → 重新建立
        if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {
            purchase.getPayments().clear(); // 清除舊資料
            Set<Payment> payments = dto.getPayments().stream()
                    .map(paymentMapper::toEntity)
                    .peek(p -> {
                        p.setId(null);           // ✅ 保證不帶舊 ID
                        p.setPurchase(purchase); // ✅ 關聯父層
                    })
                    .collect(Collectors.toSet());
            purchase.getPayments().addAll(payments);
        }
        
        // 3️⃣ 更新狀態並儲存
        updatePurchaseStatus(purchase);
        return purchaseMapper.toDto(purchaseRepository.save(purchase));
    }
    
    /**
     * 刪除進貨單（自動刪除關聯付款）
     */
    @Override
    @Transactional
    public void deletePurchase(Long id) {
        if (!purchaseRepository.existsById(id)) {
            throw new EntityNotFoundException("找不到進貨單 (ID: " + id + ")");
        }
        purchaseRepository.deleteById(id);
    }
    
    /**
     * 🧮 自動更新付款狀態
     */
    private void updatePurchaseStatus(Purchase purchase) {
        BigDecimal total = purchaseMapper.calcTotal(purchase);
        BigDecimal paid = purchaseMapper.calcPaid(purchase);
        
        if (paid.compareTo(BigDecimal.ZERO) == 0) {
            purchase.setStatus(Purchase.Status.PENDING);
        } else if (paid.compareTo(total) < 0) {
            purchase.setStatus(Purchase.Status.PARTIAL);
        } else {
            purchase.setStatus(Purchase.Status.PAID);
        }
        purchaseRepository.save(purchase);
    }
}
