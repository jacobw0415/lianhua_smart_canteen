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
    public List<PurchaseResponseDto> getAllPurchases() {
        return purchaseRepository.findAll()
                .stream()
                .map(purchaseMapper::toDto)
                .toList();
    }

    // === 查詢單筆 ===
    @Override
    public PurchaseResponseDto getPurchaseById(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的進貨單 (ID: " + id + ")"));
        return purchaseMapper.toDto(purchase);
    }

    // === 建立進貨單（含付款）===
    @Override
    @Transactional
    public PurchaseResponseDto createPurchase(PurchaseRequestDto dto) {
        try {
            // 1️⃣ 驗證供應商存在
            Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到供應商 ID：" + dto.getSupplierId()));

            // 2️⃣ 建立 Purchase 實體
            Purchase purchase = purchaseMapper.toEntity(dto);
            purchase.setSupplier(supplier);

            // 3️⃣ 計算金額（稅額 + 總額）
            computeAmounts(purchase);

            // 4️⃣ 儲存主表
            Purchase savedPurchase = purchaseRepository.save(purchase);

            // 5️⃣ 若有付款資料 → 建立關聯
            if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {
                Set<Payment> payments = dto.getPayments().stream()
                        .map(paymentMapper::toEntity)
                        .peek(p -> p.setPurchase(savedPurchase))
                        .collect(Collectors.toSet());

                paymentRepository.saveAll(payments);
                savedPurchase.setPayments(payments);
            }

            // 6️⃣ 自動更新狀態
            updatePurchaseStatus(savedPurchase);

            // 7️⃣ 回傳
            return purchaseMapper.toDto(savedPurchase);

        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("資料衝突，可能為重複的進貨紀錄或供應商錯誤。", e);
        }
    }

    // === 更新進貨單 ===
    @Override
    @Transactional
    public PurchaseResponseDto updatePurchase(Long id, PurchaseRequestDto dto) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 (ID: " + id + ")"));

        // === 1️⃣ 更新欄位 ===
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

        if (dto.getTaxRate() != null && !dto.getTaxRate().equals(purchase.getTaxRate())) {
            purchase.setTaxRate(dto.getTaxRate());
            amountChanged = true;
        }

        // === 2️⃣ 更新付款資料 ===
        if (dto.getPayments() != null) {
            if (purchase.getPayments() != null) {
                purchase.getPayments().clear();
            } else {
                purchase.setPayments(new HashSet<>());
            }

            Set<Payment> newPayments = dto.getPayments().stream()
                    .map(paymentMapper::toEntity)
                    .peek(p -> p.setPurchase(purchase))
                    .collect(Collectors.toSet());
            purchase.getPayments().addAll(newPayments);
        }

        // === 3️⃣ 若金額相關欄位有變動 → 重算金額 ===
        if (amountChanged) {
            computeAmounts(purchase);
        }

        // === 4️⃣ 自動更新狀態與金額欄位 ===
        updatePurchaseStatus(purchase);

        // === 5️⃣ 回傳 DTO ===
        return purchaseMapper.toDto(purchase);
    }

    // === 更新狀態（僅管理用途）===
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

    // === 刪除進貨單（連同付款）===
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
        return purchaseRepository.findAll().stream()
                .map(purchaseMapper::toDto)
                .toList();
    }

    // ======================================
    // 🔧 自動計算金額（含稅）
    // ======================================
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

    // ======================================
    // 🧮 自動更新付款狀態 + 金額同步
    // ======================================
    private void updatePurchaseStatus(Purchase purchase) {
        // 🔹 直接從 Mapper 計算
        BigDecimal total = purchaseMapper.calcTotal(purchase);
        BigDecimal paid = purchaseMapper.calcPaid(purchase);
        BigDecimal balance = total.subtract(paid);

        // 🔹 狀態自動更新（不需 setPaidAmount / setBalance）
        if (paid.compareTo(BigDecimal.ZERO) == 0) {
            purchase.setStatus(Purchase.Status.PENDING);
        } else if (paid.compareTo(total) < 0) {
            purchase.setStatus(Purchase.Status.PARTIAL);
        } else {
            purchase.setStatus(Purchase.Status.PAID);
        }

        // 🔹 同步付款 note
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

        // 🔹 不修改 DB，僅更新 status
        purchaseRepository.save(purchase);
    }

}
