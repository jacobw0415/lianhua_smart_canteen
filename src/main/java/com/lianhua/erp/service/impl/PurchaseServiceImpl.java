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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {
    
    private final PurchaseRepository purchaseRepository;
    private final PaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseMapper purchaseMapper;
    private final PaymentMapper paymentMapper;
    
    // === 取得所有進貨單 ===
    @Override
    @Transactional(readOnly = true)
    public List<PurchaseResponseDto> getAllPurchases() {
        return purchaseRepository.findAll()
                .stream()
                .map(purchaseMapper::toDto)
                .toList();
    }
    
    // === 查詢單筆 ===
    @Override
    @Transactional(readOnly = true)
    public PurchaseResponseDto getPurchaseById(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的進貨單 (ID: " + id + ")"));
        return purchaseMapper.toDto(purchase);
    }
    
    // === 建立進貨單（含付款金額自動運算）===
    @Override
    @Transactional
    public PurchaseResponseDto createPurchase(PurchaseRequestDto dto) {
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new EntityNotFoundException("找不到供應商 ID：" + dto.getSupplierId()));

        if (purchaseRepository.existsBySupplierIdAndPurchaseDateAndItem(
                dto.getSupplierId(), dto.getPurchaseDate(), dto.getItem())) {
            throw new IllegalArgumentException("該供應商於此日期的相同品項已存在，請勿重複建立。");
        }

        Purchase purchase = purchaseMapper.toEntity(dto);
        purchase.setSupplier(supplier);

        // 1️⃣ 計算金額（稅額 + 總額）
        computeAmounts(purchase);

        // 2️⃣ 若有付款資料 → 處理付款金額加總
        BigDecimal paidTotal = BigDecimal.ZERO;
        if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {
            Set<Payment> payments = dto.getPayments().stream()
                    .map(paymentMapper::toEntity)
                    .peek(p -> p.setPurchase(purchase))
                    .collect(Collectors.toSet());
            paidTotal = payments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            purchase.setPayments(payments);
        }

        // 3️⃣ 更新 paid_amount / balance / status
        purchase.setPaidAmount(paidTotal);
        BigDecimal balance = purchase.getTotalAmount().subtract(paidTotal).setScale(2, RoundingMode.HALF_UP);
        purchase.setBalance(balance);
        updatePurchaseStatus(purchase);

        // 4️⃣ 儲存
        try {
            Purchase saved = purchaseRepository.save(purchase);
            if (purchase.getPayments() != null && !purchase.getPayments().isEmpty()) {
                paymentRepository.saveAll(purchase.getPayments());
            }
            return purchaseMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("資料重複：該供應商於此日期的相同品項已存在。", e);
        }
    }

    
    // === 更新進貨單（含金額修改限制）===
    @Override
    @Transactional
    public PurchaseResponseDto updatePurchase(Long id, PurchaseRequestDto dto) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 (ID: " + id + ")"));

        Long supplierId = dto.getSupplierId() != null ? dto.getSupplierId() : purchase.getSupplier().getId();
        LocalDate newDate = dto.getPurchaseDate() != null ? dto.getPurchaseDate() : purchase.getPurchaseDate();
        String newItem = dto.getItem() != null ? dto.getItem() : purchase.getItem();

        boolean conflict = purchaseRepository.existsBySupplierIdAndPurchaseDateAndItemAndIdNot(
                supplierId, newDate, newItem, id);
        if (conflict) {
            throw new IllegalArgumentException("該供應商於此日期的相同品項已存在，請重新輸入。");
        }
        
        purchase.setPurchaseDate(dto.getPurchaseDate());
        purchase.setItem(dto.getItem());
        purchase.setNote(dto.getNote());
        
        boolean amountChanged = false;
        if (dto.getQty() != null && !dto.getQty().equals(purchase.getQty())) {
            purchase.setQty(dto.getQty());
            amountChanged = true;
        }
        if (dto.getUnitPrice() != null && !dto.getUnitPrice().equals(purchase.getUnitPrice())) {
            purchase.setUnitPrice(dto.getUnitPrice());
            amountChanged = true;
        }
        
        // === 🔹 處理付款金額 ===
        if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {
            Set<Payment> existingPayments = purchase.getPayments() != null
                    ? purchase.getPayments()
                    : new HashSet<>();
            
            BigDecimal newPaymentTotal = BigDecimal.ZERO;
            
            for (var paymentDto : dto.getPayments()) {
                Payment newPayment = paymentMapper.toEntity(paymentDto);
                newPayment.setPurchase(purchase);
                
                // ✅ 若 reference_no 存在，代表更新既有付款紀錄
                Optional<Payment> existing = existingPayments.stream()
                        .filter(p -> p.getReferenceNo() != null
                                && p.getReferenceNo().equalsIgnoreCase(newPayment.getReferenceNo()))
                        .findFirst();
                
                if (existing.isPresent()) {
                    // 更新舊紀錄（例如修正金額或備註）
                    existing.get().setAmount(newPayment.getAmount());
                    existing.get().setMethod(newPayment.getMethod());
                    existing.get().setNote(newPayment.getNote());
                    existing.get().setPayDate(newPayment.getPayDate());
                } else {
                    // 新增新付款紀錄
                    existingPayments.add(newPayment);
                }
                
                newPaymentTotal = newPaymentTotal.add(newPayment.getAmount());
            }
            
            // ✅ 更新總付款金額（累加舊紀錄 + 新紀錄）
            BigDecimal totalPaid = existingPayments.stream()
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // === 檢查不得超出應付款金額 ===
            if (totalPaid.compareTo(purchase.getTotalAmount()) > 0) {
                throw new IllegalArgumentException("付款金額不可超過應付總額 (" + purchase.getTotalAmount() + ")");
            }
            
            purchase.setPayments(existingPayments);
            purchase.setPaidAmount(totalPaid);
        }
        
        // === 若數量或單價有變更，重新計算總金額 ===
        if (amountChanged) {
            computeAmounts(purchase);
        }
        
        // === 同步餘額與狀態 ===
        BigDecimal balance = purchase.getTotalAmount().subtract(purchase.getPaidAmount()).setScale(2, RoundingMode.HALF_UP);
        purchase.setBalance(balance);
        updatePurchaseStatus(purchase);
        
        // === 儲存所有異動 ===
        try {
            purchaseRepository.save(purchase);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("更新失敗：該供應商於此日期的相同品項已存在。", e);
        }

        return purchaseMapper.toDto(purchase);
    }
    // === 狀態更新（不變）===
    @Override
    @Transactional
    public PurchaseResponseDto updateStatus(Long id, String status) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 (ID: " + id + ")"));
        
        try {
            Purchase.Status newStatus = Purchase.Status.valueOf(status.toUpperCase());
            purchase.setStatus(newStatus);
            Purchase updated = purchaseRepository.save(purchase);
            return purchaseMapper.toDto(updated);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("無效的狀態: " + status);
        }
    }
    
    // === 刪除進貨單 ===
    @Override
    @Transactional
    public void deletePurchase(Long id) {
        if (!purchaseRepository.existsById(id)) {
            throw new EntityNotFoundException("找不到進貨單 (ID: " + id + ")");
        }
        paymentRepository.deleteAllByPurchaseId(id);
        purchaseRepository.deleteById(id);
    }
    
    @Override
    public List<PurchaseResponseDto> findAll() {
        return List.of();
    }
    
    // === 共用稅額計算（不變）===
    private void computeAmounts(Purchase purchase) {
        if (purchase.getQty() == null || purchase.getUnitPrice() == null) {
            purchase.setTaxAmount(BigDecimal.ZERO);
            purchase.setTotalAmount(BigDecimal.ZERO);
            return;
        }
        
        BigDecimal qty = BigDecimal.valueOf(purchase.getQty());
        BigDecimal unitPrice = purchase.getUnitPrice();
        BigDecimal subtotal = unitPrice.multiply(qty);
        
        BigDecimal taxRate = purchase.getTaxRate() != null ? purchase.getTaxRate() : BigDecimal.ZERO;
        BigDecimal taxAmount = taxRate.compareTo(BigDecimal.ZERO) > 0
                ? subtotal.multiply(taxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                : BigDecimal.ZERO;
        
        BigDecimal totalAmount = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        purchase.setTaxAmount(taxAmount);
        purchase.setTotalAmount(totalAmount);
    }
    
    // === 自動更新狀態 ===
    private void updatePurchaseStatus(Purchase purchase) {
        BigDecimal total = purchase.getTotalAmount() != null ? purchase.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paid = purchase.getPaidAmount() != null ? purchase.getPaidAmount() : BigDecimal.ZERO;
        
        if (paid.compareTo(BigDecimal.ZERO) == 0) {
            purchase.setStatus(Purchase.Status.PENDING);
        } else if (paid.compareTo(total) < 0) {
            purchase.setStatus(Purchase.Status.PARTIAL);
        } else {
            purchase.setStatus(Purchase.Status.PAID);
        }
        
        if (purchase.getPayments() != null && !purchase.getPayments().isEmpty()) {
            for (Payment p : purchase.getPayments()) {
                p.setPurchase(purchase);
                switch (purchase.getStatus()) {
                    case PENDING -> p.setNote("尚未付款");
                    case PARTIAL -> p.setNote("部分付款中");
                    case PAID -> p.setNote("已全額付款");
                }
            }
        }
    }
}
