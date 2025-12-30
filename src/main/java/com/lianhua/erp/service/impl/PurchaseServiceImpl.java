package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.domain.PaymentRecordStatus;
import com.lianhua.erp.domain.PurchaseStatus;
import com.lianhua.erp.dto.purchase.*;
import com.lianhua.erp.mapper.PurchaseMapper;
import com.lianhua.erp.mapper.PaymentMapper;
import com.lianhua.erp.mapper.PurchaseItemMapper;
import com.lianhua.erp.repository.PurchaseRepository;
import com.lianhua.erp.repository.PaymentRepository;
import com.lianhua.erp.repository.SupplierRepository;
import com.lianhua.erp.service.PurchaseService;
import com.lianhua.erp.service.impl.spec.PurchaseSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseMapper purchaseMapper;
    private final PaymentMapper paymentMapper;
    private final PurchaseItemMapper purchaseItemMapper;
    private final com.lianhua.erp.numbering.PurchaseNoGenerator purchaseNoGenerator;

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
        Purchase purchase = purchaseRepository.findWithSupplierById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的進貨單 (ID: " + id + ")"));
        // 確保 items 被載入
        if (purchase.getItems() != null) {
            purchase.getItems().size(); // 觸發 lazy loading
        }
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

        // =============================================
        // 1️⃣ 基本欄位完整性檢查（主體欄位）
        // =============================================

        if (dto.getSupplierId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supplierId 為必填欄位");
        }

        if (dto.getPurchaseDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "（進貨日期）為必填欄位");
        }

        // =============================================
        // 1.1️⃣ 檢查明細列表
        // =============================================
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少需要一筆採購明細");
        }

        // 驗證每筆明細
        for (PurchaseItemRequestDto itemDto : dto.getItems()) {
            if (itemDto.getItem() == null || itemDto.getItem().trim().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "明細品項名稱不可為空");
            }
            if (itemDto.getUnit() == null || itemDto.getUnit().trim().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "明細單位不可為空");
            }
            if (itemDto.getQty() == null || itemDto.getQty() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "明細數量必須大於 0");
            }
            if (itemDto.getUnitPrice() == null || itemDto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "明細單價必須大於 0");
            }
        }

        // =============================================
        // 2️⃣ 找供應商 + 停用檢查
        // =============================================
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "找不到供應商 ID：" + dto.getSupplierId()));

        if (!Boolean.TRUE.equals(supplier.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SUPPLIER_DISABLED: 此供應商已被停用，無法建立進貨單");
        }

        // =============================================
        // 3️⃣ 移除重複檢查（因為現在支援多明細，不再需要此檢查）
        // =============================================

        // =============================================
        // 移除空白付款資訊
        // =============================================
        if (dto.getPayments() != null) {
            dto.setPayments(
                    dto.getPayments().stream()
                            .filter(p -> p.getAmount() != null ||
                                    p.getPayDate() != null ||
                                    (p.getMethod() != null && !p.getMethod().isBlank()))
                            .toList());
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
                            "付款資訊不完整：金額、付款日期與付款方式需同時填寫");
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

        // ⭐ 產生進貨單編號（商業單號）
        String purchaseNo = purchaseNoGenerator.generate(dto.getPurchaseDate());
        purchase.setPurchaseNo(purchaseNo);

        // 設定會計期間
        if (purchase.getPurchaseDate() != null) {
            purchase.setAccountingPeriod(purchase.getPurchaseDate().format(PERIOD_FORMAT));
        } else {
            purchase.setAccountingPeriod(LocalDate.now().format(PERIOD_FORMAT));
        }

        // =============================================
        // 5.1️⃣ 建立採購明細並計算總金額
        // =============================================
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurchaseItem> items = new ArrayList<>();

        for (PurchaseItemRequestDto itemDto : dto.getItems()) {
            PurchaseItem item = purchaseItemMapper.toEntity(itemDto);
            item.setPurchase(purchase);

            // 計算明細金額
            computeItemAmounts(item);
            totalAmount = totalAmount.add(item.getSubtotal());

            items.add(item);
        }

        purchase.setItems(items);
        purchase.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));

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
                                        "付款日期不得早於進貨日期 (" + purchase.getPurchaseDate() + ")");
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

            // 首筆付款不得超過應付總額（允許 0.01 的精度誤差）
            if (paidTotal.subtract(purchase.getTotalAmount()).compareTo(new BigDecimal("0.01")) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "首筆付款金額不可超過進貨應付總額 (" + purchase.getTotalAmount() + ")");
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
            log.info("建立進貨單：supplierId={}, itemsCount={}, totalAmount={}, paidAmount={}",
                    dto.getSupplierId(), items.size(), purchase.getTotalAmount(), paidTotal);

            Purchase saved = purchaseRepository.save(purchase);
            // Cascade 會自動保存 payments，不需要手動保存

            log.info("進貨單建立成功：purchaseId={}, purchaseNo={}", saved.getId(), saved.getPurchaseNo());
            return purchaseMapper.toDto(saved);

        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "資料重複：該供應商於此日期的相同品項已存在。");
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
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 (ID: " + id + ")"));

        // ========================================
        // 1️⃣ 檢查進貨單是否已作廢
        // ========================================
        if (purchase.getRecordStatus() == PurchaseStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已作廢的進貨單不可修改");
        }

        // ========================================
        // 2️⃣ 若進貨單已付清 → 禁止新增或修改付款
        // ========================================
        if (purchase.getStatus() == Purchase.Status.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "此進貨單已全額付清，不可新增或修改付款");
        }

        // ========================================
        // 2️⃣ 停用供應商不可新增付款
        // ========================================
        Supplier supplier = purchase.getSupplier();
        if (!Boolean.TRUE.equals(supplier.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SUPPLIER_DISABLED: 此供應商已被停用，無法新增付款");
        }

        // ========================================
        // 3️⃣ 固定欄位不可修改
        // ========================================
        if (dto.getPurchaseDate() != null && !dto.getPurchaseDate().equals(purchase.getPurchaseDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不允許修改進貨日期。");
        }

        // 明細不可修改（只能修改付款）
        // 注意：前端可能會發送完整的 items 數據，需要檢查是否真的被修改
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            // 載入現有的明細數據
            List<PurchaseItem> existingItems = purchase.getItems() != null ? purchase.getItems() : new ArrayList<>();

            // 檢查數量是否相同
            if (dto.getItems().size() != existingItems.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不允許修改採購明細，僅可修改付款資訊。");
            }

            // 檢查每個明細是否被修改（比較關鍵字段）
            for (int i = 0; i < dto.getItems().size(); i++) {
                PurchaseItemRequestDto dtoItem = dto.getItems().get(i);
                if (i < existingItems.size()) {
                    PurchaseItem existingItem = existingItems.get(i);

                    // 比較關鍵字段
                    boolean itemChanged = !Objects.equals(dtoItem.getItem(), existingItem.getItem()) ||
                            !Objects.equals(dtoItem.getUnit(), existingItem.getUnit()) ||
                            !Objects.equals(dtoItem.getQty(), existingItem.getQty()) ||
                            (dtoItem.getUnitPrice() != null && existingItem.getUnitPrice() != null &&
                                    dtoItem.getUnitPrice().compareTo(existingItem.getUnitPrice()) != 0);

                    if (itemChanged) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不允許修改採購明細，僅可修改付款資訊。");
                    }
                } else {
                    // 如果有新的明細項目，不允許
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不允許修改採購明細，僅可修改付款資訊。");
                }
            }
        }

        // ========================================
        // 4️⃣ 付款資料必須至少有一筆
        // ========================================
        if (dto.getPayments() == null || dto.getPayments().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "請至少新增 1 筆付款紀錄，包含完整欄位必填資訊");
        }

        // ========================================
        // 5️⃣ 付款不可全為空白欄位
        // ========================================
        boolean emptyPayment = dto.getPayments().stream().allMatch(p -> (p.getAmount() == null) &&
                p.getPayDate() == null &&
                (p.getMethod() == null || p.getMethod().isBlank()));

        if (emptyPayment) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "付款資訊不可全為空");
        }

        // ========================================
        // 6️⃣ 現有付款資料
        // ========================================
        Set<Payment> existingPayments = purchase.getPayments() != null ? purchase.getPayments() : new HashSet<>();

        BigDecimal totalAmount = purchase.getTotalAmount();

        // ⭐ 只計算有效付款（排除已作廢的）
        BigDecimal totalPaidBefore = existingPayments.stream()
                .filter(p -> p.getStatus() == PaymentRecordStatus.ACTIVE)
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ========================================
        // 7️⃣ 先驗證每筆付款欄位完整性
        // ========================================
        for (var pDto : dto.getPayments()) {

            boolean hasAny = pDto.getAmount() != null ||
                    pDto.getPayDate() != null ||
                    (pDto.getMethod() != null && !pDto.getMethod().isBlank());

            boolean hasAll = pDto.getAmount() != null &&
                    pDto.getPayDate() != null &&
                    (pDto.getMethod() != null && !pDto.getMethod().isBlank());

            if (hasAny && !hasAll) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "付款資訊不完整：金額、付款日期、付款方式需同時填寫");
            }

            // 付款日期不可早於進貨日期
            if (pDto.getPayDate() != null &&
                    pDto.getPayDate().isBefore(purchase.getPurchaseDate())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "付款日期不得早於進貨日期 (" + purchase.getPurchaseDate() + ")");
            }
        }

        // ========================================
        // 8️⃣ 逐筆處理付款（新增 or 更新）
        // ========================================
        for (var pDto : dto.getPayments()) {

            Payment newPayment = paymentMapper.toEntity(pDto);
            newPayment.setPurchase(purchase);

            // referenceNo 必須存在
            if (newPayment.getReferenceNo() == null) {
                newPayment.setReferenceNo(UUID.randomUUID().toString());
            }

            // 會計期間
            newPayment.setAccountingPeriod(
                    (newPayment.getPayDate() != null)
                            ? newPayment.getPayDate().format(PERIOD_FORMAT)
                            : LocalDate.now().format(PERIOD_FORMAT));

            Optional<Payment> existing = existingPayments.stream()
                    .filter(p -> p.getReferenceNo() != null &&
                            p.getReferenceNo().equalsIgnoreCase(newPayment.getReferenceNo()))
                    .findFirst();

            if (existing.isPresent()) {
                // ===== 更新既有付款 =====
                Payment old = existing.get();

                // ⚠️ 已作廢的付款單不可修改
                if (old.getStatus() == PaymentRecordStatus.VOIDED) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "已作廢的付款單不可修改");
                }

                BigDecimal newTotal = totalPaidBefore
                        .subtract(old.getAmount())
                        .add(newPayment.getAmount());

                if (newTotal.compareTo(totalAmount) > 0) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "更新後將超出應付總額 (" + totalAmount + ")");
                }

                old.setAmount(newPayment.getAmount());
                old.setPayDate(newPayment.getPayDate());
                old.setMethod(newPayment.getMethod());
                old.setAccountingPeriod(newPayment.getAccountingPeriod());
                old.setNote(null); // 後續自動重寫

                totalPaidBefore = newTotal;

            } else {
                // ===== 新增付款 =====
                BigDecimal newTotal = totalPaidBefore.add(newPayment.getAmount());

                // 允許 0.01 的精度誤差（因為金額計算可能有四捨五入誤差）
                if (newTotal.subtract(totalAmount).compareTo(new BigDecimal("0.01")) > 0) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "新增付款金額不得超過應付總額 (" + totalAmount + ")");
                }

                existingPayments.add(newPayment);
                totalPaidBefore = newTotal;
            }
        }

        // ========================================
        // 9️⃣ 最終安全驗證：總額不可超付（只計算有效付款）
        // ========================================
        BigDecimal totalPaid = existingPayments.stream()
                .filter(p -> p.getStatus() == PaymentRecordStatus.ACTIVE)
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 允許 0.01 的精度誤差（因為金額計算可能有四捨五入誤差）
        if (totalPaid.subtract(totalAmount).compareTo(new BigDecimal("0.01")) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "總付款金額不可超過應付總額 (" + totalAmount + ")");
        }

        // ========================================
        // 🔟 更新狀態、金額、餘額、note
        // ========================================
        purchase.setPayments(existingPayments);
        purchase.setPaidAmount(totalPaid);
        purchase.setBalance(totalAmount.subtract(totalPaid).setScale(2, RoundingMode.HALF_UP));
        updatePurchaseStatus(purchase);

        log.info("更新進貨單付款：purchaseId={}, totalPaid={}, balance={}, status={}",
                id, totalPaid, purchase.getBalance(), purchase.getStatus());

        Purchase saved = purchaseRepository.save(purchase);
        // Cascade 會自動處理 payments 的新增、更新、刪除

        return purchaseMapper.toDto(saved);
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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效的狀態: " + status);
        }
    }

    // ================================
    // 刪除進貨單（嚴格限制）
    // ================================
    @Override
    @Transactional
    public void deletePurchase(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 (ID: " + id + ")"));

        // ⚠️ 已作廢的進貨單不可刪除（應保留記錄）
        if (purchase.getRecordStatus() == PurchaseStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已作廢的進貨單不可刪除");
        }

        // ⚠️ 已有付款紀錄的進貨單不可刪除
        if (purchase.getStatus() != Purchase.Status.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已有付款紀錄的進貨單不可刪除");
        }

        log.info("刪除進貨單：purchaseId={}", id);
        purchaseRepository.deleteById(id);
        // orphanRemoval = true 會自動刪除關聯的 payments（但只有在 PENDING 狀態時才會執行到這裡）
    }

    /*
     * =======================================================
     * 📌 作廢進貨單
     * =======================================================
     */
    @Override
    @Transactional
    public PurchaseResponseDto voidPurchase(Long id, String reason) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 ID：" + id));

        // 檢查是否已作廢
        if (purchase.getRecordStatus() == PurchaseStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "此進貨單已經作廢");
        }

        // ⭐ 核心邏輯：自動作廢所有相關的有效付款單
        Set<Payment> payments = purchase.getPayments();
        if (payments != null && !payments.isEmpty()) {
            for (Payment payment : payments) {
                // 只作廢有效的付款單（跳過已作廢的）
                if (payment.getStatus() == PaymentRecordStatus.ACTIVE) {
                    payment.setStatus(PaymentRecordStatus.VOIDED);
                    payment.setVoidedAt(LocalDateTime.now());
                    // ⭐ 使用使用者輸入的作廢原因（可選），而非自動填入固定文字
                    payment.setVoidReason(reason);
                    paymentRepository.save(payment);
                    log.info("✅ 自動作廢付款單：paymentId={}, purchaseId={}, reason={}",
                            payment.getId(), id, reason);
                }
            }
        }

        // 標記進貨單為已作廢
        purchase.setRecordStatus(PurchaseStatus.VOIDED);
        purchase.setVoidedAt(LocalDateTime.now());
        purchase.setVoidReason(reason);

        // 更新付款金額（所有付款都已作廢，所以有效付款金額 = 0）
        purchase.setPaidAmount(BigDecimal.ZERO);
        // balance 是計算欄位，會自動更新
        // ⭐ 保留原有的付款狀態（PARTIAL / PAID），不自動變更為 PENDING
        // 這樣可以反映作廢前的付款記錄狀態

        purchaseRepository.save(purchase);

        log.info("✅ 作廢進貨單：purchaseId={}, reason={}", id, reason);

        // 重新查詢以確保關聯資料被載入
        Purchase savedPurchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到進貨單 ID：" + id));

        return purchaseMapper.toDto(savedPurchase);
    }

    // ================================
    // 計算明細小計（不含稅）
    // ================================
    private void computeItemAmounts(PurchaseItem item) {
        if (item.getQty() == null || item.getUnitPrice() == null) {
            item.setSubtotal(BigDecimal.ZERO);
            return;
        }

        BigDecimal qty = BigDecimal.valueOf(item.getQty());
        BigDecimal unitPrice = item.getUnitPrice();

        // 小計 = 單價 × 數量（不含稅）
        BigDecimal subtotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);

        item.setSubtotal(subtotal);
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
        boolean empty = isEmpty(req.getSupplierName()) &&
                req.getSupplierId() == null &&
                isEmpty(req.getItem()) &&
                isEmpty(req.getStatus()) &&
                isEmpty(req.getAccountingPeriod()) &&
                isEmpty(req.getPurchaseNo()) &&
                isEmpty(req.getFromDate()) &&
                isEmpty(req.getToDate());

        if (empty) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜尋條件不可全為空，至少需提供一項搜尋欄位");
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
                    "無效排序欄位：" + ex.getPropertyName());
        }

        return result.map(purchaseMapper::toResponseDto);
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
