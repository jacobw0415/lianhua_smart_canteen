package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    @Transactional(readOnly = true)
    @Override
    public Page<PurchaseResponseDto> getAllPurchases(Pageable pageable) {
        Pageable safePageable = normalizePageable(pageable);
        try {
            return purchaseRepository.findAll(safePageable).map(purchaseMapper::toDto);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "無效排序欄位：" + ex.getPropertyName());
        }
    }

    // ================================================================
    // 分頁防呆 + 預設排序
    // ================================================================
    private Pageable normalizePageable(Pageable pageable) {

        if (pageable.getPageNumber() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 不可小於 0");
        }

        if (pageable.getPageSize() <= 0 || pageable.getPageSize() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size 需介於 1 - 200 之間");
        }

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.ASC, "id");

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
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

    /* =====================================================
       ⭐ 1️⃣ 新增付款欄位必填規則（非常重要）
       — 金額 / 日期 / 付款方式 必須同時存在或同時不存在
    ====================================================== */
        if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {

            dto.getPayments().forEach(p -> {

                boolean hasAmount = p.getAmount() != null;
                boolean hasDate   = p.getPayDate() != null;
                boolean hasMethod = p.getMethod() != null && !p.getMethod().isBlank();

                // 三者任意一個有填 → 必須三個都要填
                if ((hasAmount || hasDate || hasMethod) &&
                        !(hasAmount && hasDate && hasMethod)) {

                    throw new IllegalArgumentException("付款資訊不完整：金額、付款日期、付款方式需同時填寫。");
                }
            });
        }

    /* =====================================================
       2️⃣ 原本流程：建立 purchase entity
    ====================================================== */
        Purchase purchase = purchaseMapper.toEntity(dto);
        purchase.setSupplier(supplier);

        // 設定會計期間
        if (purchase.getPurchaseDate() != null) {
            purchase.setAccountingPeriod(purchase.getPurchaseDate().format(PERIOD_FORMAT));
        } else {
            purchase.setAccountingPeriod(LocalDate.now().format(PERIOD_FORMAT));
        }

        // 計算金額
        computeAmounts(purchase);

    /* =====================================================
       ⭐ 3️⃣ 付款日期不可早於進貨日期
       並重新確認付款欄位
    ====================================================== */

        BigDecimal paidTotal = BigDecimal.ZERO;

        if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {
            Set<Payment> payments = dto.getPayments().stream()
                    .map(paymentMapper::toEntity)
                    .peek(p -> {

                        p.setPurchase(purchase);

                        if (p.getPayDate() != null) {

                            // 日期不可早於進貨
                            if (purchase.getPurchaseDate() != null &&
                                    p.getPayDate().isBefore(purchase.getPurchaseDate())) {
                                throw new IllegalArgumentException(
                                        STR."付款日期不得早於進貨日期 (\{purchase.getPurchaseDate()})");
                            }

                            // 設定付款會計期間
                            p.setAccountingPeriod(p.getPayDate().format(PERIOD_FORMAT));
                        } else {
                            p.setAccountingPeriod(LocalDate.now().format(PERIOD_FORMAT));
                        }

                    })
                    .collect(Collectors.toSet());

            paidTotal = payments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 首筆付款不可大於總金額
            if (paidTotal.compareTo(purchase.getTotalAmount()) > 0) {
                throw new IllegalArgumentException(
                        STR."首筆付款金額不可超過進貨應付總額 (\{purchase.getTotalAmount()})");
            }

            purchase.setPayments(payments);
        }

        // 更新應付、狀態
        purchase.setPaidAmount(paidTotal);

        BigDecimal balance = purchase.getTotalAmount()
                .subtract(paidTotal)
                .setScale(2, RoundingMode.HALF_UP);

        purchase.setBalance(balance);
        updatePurchaseStatus(purchase);

        // 儲存
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

    // ==== 更新進貨單（含金額修改與會計期間重新判定）===
    @Override
    @Transactional
    public PurchaseResponseDto updatePurchase(Long id, PurchaseRequestDto dto) {

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 (ID: " + id + ")"));

        // === 嚴格限制不可改欄位 ===
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

        // === 僅允許修改付款資訊 ===
        if (dto.getPayments() == null || dto.getPayments().isEmpty()) {
            throw new IllegalArgumentException("付款資訊不完整：金額、付款日期、付款方式需同時填寫。");
        }

        Set<Payment> existingPayments =
                purchase.getPayments() != null ? purchase.getPayments() : new HashSet<>();

        BigDecimal totalPaid = existingPayments.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unpaid = purchase.getTotalAmount().subtract(totalPaid);

        // ============================================================
        // ⭐ 1️⃣ 第一時間前置驗證（與 createPurchase 完全一致）
        // ============================================================
        for (var p : dto.getPayments()) {

            boolean hasAmount = p.getAmount() != null;
            boolean hasDate   = p.getPayDate() != null;
            boolean hasMethod = p.getMethod() != null && !p.getMethod().isBlank();

            // 任意一項存在 → 三項都必須存在
            if ((hasAmount || hasDate || hasMethod) &&
                    !(hasAmount && hasDate && hasMethod)) {

                throw new IllegalArgumentException("付款資訊不完整：金額、付款日期、付款方式需同時填寫。");
            }

            // === 付款日期驗證 ===
            if (p.getPayDate() != null &&
                    p.getPayDate().isBefore(purchase.getPurchaseDate())) {
                throw new IllegalArgumentException(
                        "付款日期不得早於進貨日期 (" + purchase.getPurchaseDate() + ")"
                );
            }

            // === 金額驗證（最重要） ===
            if (p.getAmount() != null && p.getAmount().compareTo(unpaid) > 0) {
                throw new IllegalArgumentException(
                        "付款金額不可超過尚未付款金額 (" + unpaid + ")"
                );
            }
        }

        // ============================================================
        // ⭐ 2️⃣ 通過前置驗證後 → 正式處理付款（原本 update 邏輯）
        // ============================================================
        for (var paymentDto : dto.getPayments()) {
            Payment newPayment = paymentMapper.toEntity(paymentDto);
            newPayment.setPurchase(purchase);

            // 設定會計期間
            if (newPayment.getPayDate() != null) {
                newPayment.setAccountingPeriod(newPayment.getPayDate().format(PERIOD_FORMAT));
            } else {
                newPayment.setAccountingPeriod(LocalDate.now().format(PERIOD_FORMAT));
            }

            Optional<Payment> existing = existingPayments.stream()
                    .filter(p -> p.getReferenceNo() != null &&
                            p.getReferenceNo().equalsIgnoreCase(newPayment.getReferenceNo()))
                    .findFirst();

            if (existing.isPresent()) {
                Payment old = existing.get();
                BigDecimal diff = newPayment.getAmount().subtract(old.getAmount());

                if (diff.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(unpaid) > 0) {
                    throw new IllegalArgumentException(
                            "付款金額不可超過尚未付款金額 (" + unpaid + ")"
                    );
                }

                old.setAmount(newPayment.getAmount());
                unpaid = unpaid.subtract(diff.max(BigDecimal.ZERO));

            } else {
                existingPayments.add(newPayment);
                unpaid = unpaid.subtract(newPayment.getAmount());
            }
        }

        // 最終金額驗證
        BigDecimal totalPaidAfter = existingPayments.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaidAfter.compareTo(purchase.getTotalAmount()) > 0) {
            throw new IllegalArgumentException(
                    "總付款金額不可超過應付總額 (" + purchase.getTotalAmount() + ")"
            );
        }

        // 更新狀態
        purchase.setPayments(existingPayments);
        purchase.setPaidAmount(totalPaidAfter);
        purchase.setBalance(
                purchase.getTotalAmount().subtract(totalPaidAfter).setScale(2, RoundingMode.HALF_UP));
        updatePurchaseStatus(purchase);

        // 儲存
        try {
            Purchase updated = purchaseRepository.save(purchase);
            paymentRepository.saveAll(purchase.getPayments());
            return purchaseMapper.toDto(updated);

        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("資料重複或外鍵錯誤", e);
        }
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
