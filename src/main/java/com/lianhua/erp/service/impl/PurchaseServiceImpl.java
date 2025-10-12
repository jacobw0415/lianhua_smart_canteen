package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.*;
import com.lianhua.erp.dto.purchase.*;
import com.lianhua.erp.mapper.PurchaseMapper;
import com.lianhua.erp.mapper.PaymentMapper;
import com.lianhua.erp.repository.PurchaseRepository;
import com.lianhua.erp.repository.PaymentRepository;
import com.lianhua.erp.repository.SupplierRepository;
import com.lianhua.erp.service.PurchaseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {
    
    private final PurchaseRepository purchaseRepository;
    private final PaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseMapper purchaseMapper;
    private final PaymentMapper paymentMapper;
    
    // 取得所有進貨單
    @Override
    public List<PurchaseResponseDto> getAllPurchases() {
        return purchaseRepository.findAll().stream()
                .map(purchaseMapper::toDto)
                .toList();
    }
    
    // 查詢單筆
    @Override
    public PurchaseResponseDto getPurchaseById(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的進貨單 (ID: " + id + ")"));
        return purchaseMapper.toDto(purchase);
    }
    
    // 建立進貨單（含付款）
    @Override
    @Transactional
    public PurchaseResponseDto createPurchase(PurchaseRequestDto dto) {
        try {
            // 1️⃣ 檢查供應商存在
            Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到供應商 ID：" + dto.getSupplierId()));
            
            // 2️⃣ 建立 Purchase 實體
            Purchase purchase = purchaseMapper.toEntity(dto);
            purchase.setSupplier(supplier);
            
            // 3️⃣ 先儲存進貨單
            Purchase savedPurchase = purchaseRepository.save(purchase);
            
            // 4️⃣ 若有付款資料，建立並關聯
            if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {
                Set<Payment> payments = dto.getPayments().stream()
                        .map(paymentMapper::toEntity)
                        .peek(p -> p.setPurchase(savedPurchase))
                        .collect(Collectors.toSet());
                
                paymentRepository.saveAll(payments);
                savedPurchase.setPayments(payments);
            }
            
            // 5️⃣ 自動更新付款狀態
            updatePurchaseStatus(savedPurchase);
            
            // 6️⃣ 回傳 DTO
            return purchaseMapper.toDto(savedPurchase);
            
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("資料衝突，可能為重複的進貨紀錄或供應商錯誤。");
        }
    }
    
    // 更新進貨單
    @Override
    @Transactional
    public PurchaseResponseDto updatePurchase(Long id, PurchaseRequestDto dto) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 (ID: " + id + ")"));
        
        // === 1️⃣ 更新基本欄位 ===
        purchase.setPurchaseDate(dto.getPurchaseDate());
        purchase.setItem(dto.getItem());
        purchase.setQty(dto.getQty());
        purchase.setUnitPrice(dto.getUnitPrice());
        purchase.setTaxRate(dto.getTaxRate());
        purchase.setNote(dto.getNote());
        
        // ⚠️ 不允許外部直接改狀態，交給系統自動更新
        // if (dto.getStatus() != null) purchase.setStatus(...); ❌ 移除這行
        
        // === 2️⃣ 更新付款集合 ===
        if (dto.getPayments() != null) {
            // 先清空舊付款（讓 JPA 自動刪除孤兒）
            if (purchase.getPayments() != null) {
                purchase.getPayments().clear();
            } else {
                purchase.setPayments(new HashSet<>());
            }
            
            // 建立新的付款集合
            Set<Payment> newPayments = dto.getPayments().stream()
                    .map(paymentMapper::toEntity)
                    .peek(p -> p.setPurchase(purchase))
                    .collect(Collectors.toSet());
            
            purchase.getPayments().addAll(newPayments);
        }
        
        // === 3️⃣ 儲存並根據付款金額自動更新狀態 ===
        updatePurchaseStatus(purchase);
        
        // === 4️⃣ 回傳 DTO ===
        return purchaseMapper.toDto(purchase);
    }
    
    
    @Override
    @Transactional
    public PurchaseResponseDto updateStatus(Long id, String status) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 (ID: " + id + ")"));
        
        try {
            Purchase.Status newStatus = Purchase.Status.valueOf(status.toUpperCase());
            purchase.setStatus(newStatus);
            Purchase updatedPurchase = purchaseRepository.save(purchase);
            return purchaseMapper.toDto(updatedPurchase);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("無效的狀態: " + status);
        }
    }
    
    // 刪除進貨單（連同付款）
    @Override
    @Transactional
    public void deletePurchase(Long id) {
        if (!purchaseRepository.existsById(id))
            throw new EntityNotFoundException("找不到進貨單 (ID: " + id + ")");
        paymentRepository.deleteAllByPurchaseId(id);
        purchaseRepository.deleteById(id);
    }
    
    @Override
    public List<PurchaseResponseDto> findAll() {
        return List.of();
    }
    
    
    // 🧮 自動更新狀態
    private void updatePurchaseStatus(Purchase purchase) {
        BigDecimal total = purchaseMapper.calcTotal(purchase);
        BigDecimal paid = purchaseMapper.calcPaid(purchase);
        
        if (paid == null) paid = BigDecimal.ZERO;
        if (total == null) total = BigDecimal.ZERO;
        
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
