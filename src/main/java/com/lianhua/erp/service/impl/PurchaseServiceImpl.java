package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.dto.purchase.*;
import com.lianhua.erp.mapper.PurchaseMapper;
import com.lianhua.erp.mapper.PaymentMapper;
import com.lianhua.erp.repository.PurchaseRepository;
import com.lianhua.erp.repository.PaymentRepository;
import com.lianhua.erp.repository.SupplierRepository;
import com.lianhua.erp.service.PurchaseService;
import com.lianhua.erp.service.impl.spec.PurchaseSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    // ================================
    // 取得所有進貨單（含分頁與排序防呆）
    // ================================
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

    // ================================
    // 分頁防呆與預設排序
    // ================================
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

    // ================================
    // 查詢單筆進貨單
    // ================================
    @Override
    @Transactional(readOnly = true)
    public PurchaseResponseDto getPurchaseById(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的進貨單 (ID: " + id + ")"));
        return purchaseMapper.toDto(purchase);
    }

    // ================================
    // 建立進貨單（含付款檢查與會計期間設定）
    // ================================
    @Override
    @Transactional
    public PurchaseResponseDto createPurchase(PurchaseRequestDto dto) {

        // =============================================
        // 0️⃣ 前置：字串去空白、自帶 DTO 基本驗證
        // =============================================
        dto.trimAll();
        dto.validateSelf();

        // 商品名稱正規化
        String normalizedItem = dto.getItem() != null ? dto.getItem().trim() : null;
        dto.setItem(normalizedItem);


        // =============================================
        // 1️⃣ 基本欄位完整性檢查（主體欄位）
        // =============================================

        if (dto.getSupplierId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supplierId 為必填欄位");
        }

        if (normalizedItem == null || normalizedItem.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "（品項名稱）為必填欄位");
        }

        if (dto.getQty() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "（數量）為必填欄位");
        }

        if (dto.getQty() <= 0) {
            throw new IllegalArgumentException("數量必須大於 0");
        }

        if (dto.getUnitPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "（單價）為必填欄位");
        }
        if (dto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "單價必須大於 0");
        }

        if (dto.getPurchaseDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "（進貨日期）為必填欄位");
        }

        // =============================================
        // 2️⃣ 找供應商 + 停用檢查
        // =============================================
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, STR."找不到供應商 ID：\{dto.getSupplierId()}"
                ));

        if (!Boolean.TRUE.equals(supplier.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SUPPLIER_DISABLED: 此供應商已被停用，無法建立進貨單"
            );
        }


        // =============================================
        // 3️⃣ 同供應商 + 日期 + 品項 不可重複
        // =============================================
        if (purchaseRepository.existsBySupplierIdAndPurchaseDateAndItem(
                dto.getSupplierId(),
                dto.getPurchaseDate(),
                normalizedItem
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "該供應商於此日期的相同品項已存在，請勿重複建立。"
            );
        }

        // =============================================
        //  移除空白付款資訊
        // =============================================
        if (dto.getPayments() != null) {
            dto.setPayments(
                    dto.getPayments().stream()
                            .filter(p ->
                                    p.getAmount() != null ||
                                            p.getPayDate() != null ||
                                            (p.getMethod() != null && !p.getMethod().isBlank())
                            )
                            .toList()
            );
        }

        // =============================================
        // 4️⃣ 付款欄位完整性檢查（強化版）
        // =============================================
        if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {

            dto.getPayments().forEach(p -> {

                boolean hasAmount = p.getAmount() != null;
                boolean hasDate = p.getPayDate() != null;
                boolean hasMethod = p.getMethod() != null && !p.getMethod().isBlank();

                // 必須全部都有或全部沒有，不可部分填寫
                if ((hasAmount || hasDate || hasMethod) &&
                        !(hasAmount && hasDate && hasMethod)) {

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "付款資訊不完整：金額、付款日期與付款方式需同時填寫"
                    );
                }

                // 金額 > 0
                if (hasAmount && p.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "付款金額必須大於 0");
                }

                // 付款日期不得晚於今天（避免未來付款）
                if (hasDate && p.getPayDate().isAfter(LocalDate.now())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "付款日期不可為未來日期");
                }
            });
        }


        // =============================================
        // 5️⃣ 建立 Purchase Entity
        // =============================================
        Purchase purchase = purchaseMapper.toEntity(dto);
        purchase.setSupplier(supplier);
        purchase.setItem(normalizedItem);

        // 設定會計期間
        if (purchase.getPurchaseDate() != null) {
            purchase.setAccountingPeriod(purchase.getPurchaseDate().format(PERIOD_FORMAT));
        } else {
            purchase.setAccountingPeriod(LocalDate.now().format(PERIOD_FORMAT));
        }

        computeAmounts(purchase);


        // =============================================
        // 6️⃣ 付款資料：日期不得早於進貨日 + 會計期間設定
        // =============================================
        BigDecimal paidTotal = BigDecimal.ZERO;

        if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {

            Set<Payment> payments = dto.getPayments().stream()
                    .map(paymentMapper::toEntity)
                    .peek(p -> {

                        p.setPurchase(purchase);

                        if (p.getPayDate() != null) {

                            // 付款日期 < 進貨日期 → 錯誤
                            if (purchase.getPurchaseDate() != null &&
                                    p.getPayDate().isBefore(purchase.getPurchaseDate())) {

                                throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        STR."付款日期不得早於進貨日期 (\{purchase.getPurchaseDate()})"
                                );
                            }

                            p.setAccountingPeriod(p.getPayDate().format(PERIOD_FORMAT));
                        } else {
                            p.setAccountingPeriod(LocalDate.now().format(PERIOD_FORMAT));
                        }
                    })
                    .collect(Collectors.toSet());

            // 總付款金額計算
            paidTotal = payments.stream()
                    .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 首筆付款不得超過應付總額
            if (paidTotal.compareTo(purchase.getTotalAmount()) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        STR."首筆付款金額不可超過進貨應付總額 (\{purchase.getTotalAmount()})"
                );
            }

            purchase.setPayments(payments);
        }

        // 設定付款總額與餘額
        purchase.setPaidAmount(paidTotal);
        BigDecimal balance = purchase.getTotalAmount()
                .subtract(paidTotal)
                .setScale(2, RoundingMode.HALF_UP);
        purchase.setBalance(balance);

        updatePurchaseStatus(purchase);


        // =============================================
        // 7️⃣ 儲存
        // =============================================
        try {
            Purchase saved = purchaseRepository.save(purchase);

            if (purchase.getPayments() != null && !purchase.getPayments().isEmpty()) {
                paymentRepository.saveAll(purchase.getPayments());
            }

            return purchaseMapper.toDto(saved);

        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "資料重複：該供應商於此日期的相同品項已存在。"
            );
        }
    }

    // ================================
    // 更新進貨單（僅允許修改付款資訊）
    // ================================
    @Override
    @Transactional
    public PurchaseResponseDto updatePurchase(Long id, PurchaseRequestDto dto) {

        dto.trimAll();
        dto.validateSelf();

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(STR."找不到進貨單 (ID: \{id})"));

        Supplier supplier = purchase.getSupplier();

        //  新增供應商停用檢查
        if (!Boolean.TRUE.equals(supplier.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SUPPLIER_DISABLED: 此供應商已被停用，無法建立進貨單"
            );
        }

        // 固定欄位不可修改
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

        // 未提供付款資料 → 視為不完整
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

        // ---------------------------------------------------------
        // 1️⃣ 前置驗證：與 createPurchase 的邏輯完全一致
        // ---------------------------------------------------------
        for (var p : dto.getPayments()) {

            boolean hasAmount = p.getAmount() != null;
            boolean hasDate = p.getPayDate() != null;
            boolean hasMethod = p.getMethod() != null && !p.getMethod().isBlank();

            if ((hasAmount || hasDate || hasMethod) &&
                    !(hasAmount && hasDate && hasMethod)) {

                throw new IllegalArgumentException("付款資訊不完整：金額、付款日期、付款方式需同時填寫。");
            }

            if (p.getPayDate() != null &&
                    p.getPayDate().isBefore(purchase.getPurchaseDate())) {
                throw new IllegalArgumentException(
                        "付款日期不得早於進貨日期 (" + purchase.getPurchaseDate() + ")"
                );
            }

            if (p.getAmount() != null && p.getAmount().compareTo(unpaid) > 0) {
                throw new IllegalArgumentException(
                        "付款金額不可超過尚未付款金額 (" + unpaid + ")"
                );
            }
        }

        // ---------------------------------------------------------
        // 2️⃣ 通過前置驗證後 → 正式進行付款更新處理
        // ---------------------------------------------------------
        for (var paymentDto : dto.getPayments()) {
            Payment newPayment = paymentMapper.toEntity(paymentDto);
            newPayment.setPurchase(purchase);

            // 付款會計期間
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
                BigDecimal amt = newPayment.getAmount() == null ? BigDecimal.ZERO : newPayment.getAmount();
                unpaid = unpaid.subtract(amt);
            }
        }

        // 最終金額驗證
        BigDecimal totalPaidAfter = existingPayments.stream()
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaidAfter.compareTo(purchase.getTotalAmount()) > 0) {
            throw new IllegalArgumentException(
                    "總付款金額不可超過應付總額 (" + purchase.getTotalAmount() + ")"
            );
        }

        // 更新狀態與金額
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

    // ================================
    // 更新進貨單狀態
    // ================================
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

    // ================================
    // 刪除進貨單
    // ================================
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

    // ================================
    // 計算稅額與總金額
    // ================================
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

    // ================================
    // 自動更新進貨單狀態與付款 note
    // ================================
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

    // ================================
    // 進貨搜尋（支援動態 Specification）
    // ================================
    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseResponseDto> searchPurchases(PurchaseSearchRequest req, Pageable pageable) {

        // =======================================================
        // ★ 1. 檢查搜尋條件是否全為空（後端防呆）
        // =======================================================
        boolean empty =
                isEmpty(req.getSupplierName()) &&
                        req.getSupplierId() == null &&
                        isEmpty(req.getItem()) &&
                        isEmpty(req.getStatus()) &&
                        isEmpty(req.getAccountingPeriod()) &&
                        isEmpty(req.getFromDate()) &&
                        isEmpty(req.getToDate());

        if (empty) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜尋條件不可全為空，至少需提供一項搜尋欄位"
            );
        }

        // =======================================================
        // ★ 2. 正常情況 → 使用 Specification 查詢
        // =======================================================
        Specification<Purchase> spec = PurchaseSpecifications.build(req);

        Page<Purchase> result;

        try {
            result = purchaseRepository.findAll(spec, pageable);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效排序欄位：" + ex.getPropertyName()
            );
        }

        return result.map(purchaseMapper::toResponseDto);
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
