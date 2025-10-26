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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseServiceImpl implements PurchaseService {
    
    private final PurchaseRepository purchaseRepository;
    private final PaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseMapper purchaseMapper;
    private final PaymentMapper paymentMapper;
    
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    
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
    
    // === 建立進貨單（含付款金額自動運算與會計期間）===
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
        
        // ✅ 1️⃣ 設定會計期間（依進貨日期）
        if (purchase.getPurchaseDate() != null) {
            purchase.setAccountingPeriod(purchase.getPurchaseDate().format(PERIOD_FORMAT));
        } else {
            purchase.setAccountingPeriod(LocalDate.now().format(PERIOD_FORMAT));
        }
        
        // 2️⃣ 計算金額
        computeAmounts(purchase);
        
        // 3️⃣ 若有付款資料 → 處理付款金額加總與會計期間
        BigDecimal paidTotal = BigDecimal.ZERO;
        if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {
            Set<Payment> payments = dto.getPayments().stream()
                    .map(paymentMapper::toEntity)
                    .peek(p -> {
                        p.setPurchase(purchase);
                        // ✅ 付款的會計期間依付款日期決定
                        if (p.getPayDate() != null) {
                            p.setAccountingPeriod(p.getPayDate().format(PERIOD_FORMAT));
                        } else {
                            p.setAccountingPeriod(LocalDate.now().format(PERIOD_FORMAT));
                        }
                    })
                    .collect(Collectors.toSet());
            
            paidTotal = payments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            purchase.setPayments(payments);
        }
        
        // 4️⃣ 更新 paid_amount / balance / status
        purchase.setPaidAmount(paidTotal);
        BigDecimal balance = purchase.getTotalAmount()
                .subtract(paidTotal)
                .setScale(2, RoundingMode.HALF_UP);
        purchase.setBalance(balance);
        updatePurchaseStatus(purchase);
        
        // 5️⃣ 儲存
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
    
    // === 更新進貨單（含金額修改與會計期間重新判定）===
    @Override
    @Transactional
    public PurchaseResponseDto updatePurchase(Long id, PurchaseRequestDto dto) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 (ID: " + id + ")"));
        
<<<<<<< HEAD
        Long supplierId = dto.getSupplierId() != null ? dto.getSupplierId() : purchase.getSupplier().getId();
        LocalDate newDate = dto.getPurchaseDate() != null ? dto.getPurchaseDate() : purchase.getPurchaseDate();
        String newItem = dto.getItem() != null ? dto.getItem() : purchase.getItem();
        
        boolean conflict = purchaseRepository.existsBySupplierIdAndPurchaseDateAndItemAndIdNot(
                supplierId, newDate, newItem, id);
        if (conflict) {
            throw new IllegalArgumentException("該供應商於此日期的相同品項已存在，請重新輸入。");
        }
        
        // === 更新基本欄位 ===
        if (dto.getPurchaseDate() != null) {
            purchase.setPurchaseDate(dto.getPurchaseDate());
            // ✅ 同步更新會計期間
            purchase.setAccountingPeriod(dto.getPurchaseDate().format(PERIOD_FORMAT));
        }
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
        
        // === 處理付款紀錄 ===
=======
        // === ⚙️ 嚴格限制但允許相同值 ===
        if (dto.getItem() != null && !dto.getItem().equals(purchase.getItem())) {
            throw new IllegalArgumentException("不允許修改品名。");
        }
        if (dto.getQty() != null && dto.getQty().compareTo(purchase.getQty()) != 0) {
            throw new IllegalArgumentException("不允許修改數量。");
        }
        if (dto.getUnitPrice() != null && dto.getUnitPrice().compareTo(purchase.getUnitPrice()) != 0) {
            throw new IllegalArgumentException("不允許修改單價。");
        }
        if (dto.getPurchaseDate() != null && !dto.getPurchaseDate().equals(purchase.getPurchaseDate())) {
            throw new IllegalArgumentException("不允許修改進貨日期。");
        }
        
        // === ⚙️ 僅允許修改付款金額 ===
>>>>>>> jacob
        if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {
            Set<Payment> existingPayments = purchase.getPayments() != null
                    ? purchase.getPayments()
                    : new HashSet<>();
            
<<<<<<< HEAD
            for (var paymentDto : dto.getPayments()) {
                Payment newPayment = paymentMapper.toEntity(paymentDto);
                newPayment.setPurchase(purchase);
                
                // ✅ 若付款日期更新 → 自動更新會計期間
                if (newPayment.getPayDate() != null) {
                    newPayment.setAccountingPeriod(newPayment.getPayDate().format(PERIOD_FORMAT));
                } else {
                    newPayment.setAccountingPeriod(LocalDate.now().format(PERIOD_FORMAT));
                }
                
                Optional<Payment> existing = existingPayments.stream()
                        .filter(p -> p.getReferenceNo() != null
                                && p.getReferenceNo().equalsIgnoreCase(newPayment.getReferenceNo()))
                        .findFirst();
                
                if (existing.isPresent()) {
                    Payment old = existing.get();
                    old.setAmount(newPayment.getAmount());
                    old.setMethod(newPayment.getMethod());
                    old.setNote(newPayment.getNote());
                    old.setPayDate(newPayment.getPayDate());
                    old.setAccountingPeriod(newPayment.getAccountingPeriod());
                } else {
                    existingPayments.add(newPayment);
                }
            }
            
            // ✅ 更新總付款金額
=======
>>>>>>> jacob
            BigDecimal totalPaid = existingPayments.stream()
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
<<<<<<< HEAD
            if (totalPaid.compareTo(purchase.getTotalAmount()) > 0) {
                throw new IllegalArgumentException("付款金額不可超過應付總額 (" + purchase.getTotalAmount() + ")");
=======
            BigDecimal unpaid = purchase.getTotalAmount().subtract(totalPaid);
            
            for (var paymentDto : dto.getPayments()) {
                Payment newPayment = paymentMapper.toEntity(paymentDto);
                newPayment.setPurchase(purchase);
                
                // ✅ 檢查付款日期是否早於進貨日期
                if (newPayment.getPayDate() != null &&
                        newPayment.getPayDate().isBefore(purchase.getPurchaseDate())) {
                    throw new IllegalArgumentException(
                            "付款日期不得早於進貨日期 (" + purchase.getPurchaseDate() + ")");
                }
                
                Optional<Payment> existing = existingPayments.stream()
                        .filter(p -> p.getReferenceNo() != null &&
                                p.getReferenceNo().equalsIgnoreCase(newPayment.getReferenceNo()))
                        .findFirst();
                
                if (existing.isPresent()) {
                    Payment old = existing.get();
                    
                    // ✅ 僅允許修改金額
                    if (newPayment.getAmount() != null &&
                            newPayment.getAmount().compareTo(old.getAmount()) != 0) {
                        
                        BigDecimal diff = newPayment.getAmount().subtract(old.getAmount());
                        if (diff.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(unpaid) > 0) {
                            throw new IllegalArgumentException(
                                    "付款金額不可超過尚未付款金額 (" + unpaid + ")");
                        }
                        old.setAmount(newPayment.getAmount());
                        unpaid = unpaid.subtract(diff.max(BigDecimal.ZERO));
                    }
                } else {
                    // ✅ 新增付款也要檢查金額與日期
                    if (newPayment.getAmount().compareTo(unpaid) > 0) {
                        throw new IllegalArgumentException(
                                "新增付款金額不可超過尚未付款金額 (" + unpaid + ")");
                    }
                    existingPayments.add(newPayment);
                    unpaid = unpaid.subtract(newPayment.getAmount());
                }
            }
            
            // ✅ 總額檢查
            BigDecimal totalPaidAfter = existingPayments.stream()
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalPaidAfter.compareTo(purchase.getTotalAmount()) > 0) {
                throw new IllegalArgumentException(
                        "總付款金額不可超過應付總額 (" + purchase.getTotalAmount() + ")");
>>>>>>> jacob
            }
            
            purchase.setPayments(existingPayments);
            purchase.setPaidAmount(totalPaidAfter);
            purchase.setBalance(
                    purchase.getTotalAmount().subtract(totalPaidAfter).setScale(2, RoundingMode.HALF_UP));
            updatePurchaseStatus(purchase);
        } else {
            throw new IllegalArgumentException("本操作僅允許修改付款金額，請提供付款資料。");
        }
        
<<<<<<< HEAD
        // === 若數量或單價有變更，重新計算金額 ===
        if (amountChanged) {
            computeAmounts(purchase);
        }
        
        // === 同步餘額與狀態 ===
        BigDecimal balance = purchase.getTotalAmount()
                .subtract(purchase.getPaidAmount())
                .setScale(2, RoundingMode.HALF_UP);
        purchase.setBalance(balance);
        updatePurchaseStatus(purchase);
        
=======
        // === 儲存異動 ===
>>>>>>> jacob
        try {
            Purchase updated = purchaseRepository.save(purchase);
            return purchaseMapper.toDto(updated);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("更新失敗：資料完整性錯誤。", e);
        }
<<<<<<< HEAD
        
        return purchaseMapper.toDto(purchase);
    }
    
    // === 狀態更新 ===
=======
    }
    
    
    // === 狀態更新（不變）===
>>>>>>> jacob
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
    
    // === 稅額計算 ===
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
