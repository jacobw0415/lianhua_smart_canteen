package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.payment.PaymentResponseDto;
import com.lianhua.erp.dto.payment.PaymentSearchRequest;
import com.lianhua.erp.mapper.PaymentMapper;
import com.lianhua.erp.repository.PaymentRepository;
import com.lianhua.erp.repository.PurchaseRepository;
import com.lianhua.erp.service.PaymentService;
import com.lianhua.erp.service.impl.spec.PaymentSpecifications;
import com.lianhua.erp.export.ExportFilenameUtils;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import com.lianhua.erp.export.TabularExporter;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final PaymentMapper paymentMapper;

    private static final String[] PAYMENT_EXPORT_HEADERS = new String[]{
            "進貨單編號",
            "供應商",
            "品項摘要",
            "付款方式",
            "付款金額",
            "付款日期",
            "會計期間",
            "付款狀態",
            "備註"
    };

    @org.springframework.beans.factory.annotation.Value("${app.export.max-rows:50000}")
    private int maxExportRows;

    /* =======================================================
     * 📌 React-Admin PaymentList 使用：分頁查詢所有付款紀錄
     * ======================================================= */
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> findAll(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(paymentMapper::toDto);
    }

    /* =======================================================
     * 📌 依進貨單 ID 查付款紀錄（原邏輯保留）
     * ======================================================= */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto findByPurchaseId(Long purchaseId) {
        return paymentRepository.findByPurchaseId(purchaseId).stream()
                .findFirst()
                .map(paymentMapper::toDto)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "找不到指定進貨單的付款紀錄，purchaseId=" + purchaseId));
    }

    /* =======================================================
     * 📌 刪除某進貨單的所有付款紀錄
     * ======================================================= */
    @Override
    @Transactional
    public void deleteByPurchaseId(Long purchaseId) {
        // ⚠️ 檢查是否有已作廢的付款，如果有則不可刪除（應保留記錄）
        List<Payment> payments = paymentRepository.findByPurchaseId(purchaseId);
        boolean hasVoided = payments.stream()
                .anyMatch(p -> p.getStatus() == PaymentRecordStatus.VOIDED);
        
        if (hasVoided) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "進貨單包含已作廢的付款記錄，不可刪除。請先處理作廢的付款記錄。");
        }
        
        paymentRepository.deleteByPurchaseId(purchaseId);
    }

    /* =======================================================
     * 📌 付款搜尋（支援動態 Specification）
     * ======================================================= */
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> searchPayments(PaymentSearchRequest req, Pageable pageable) {

        // ===== 1. 搜尋條件不可全為空 =====
        // includeVoided 不計入搜尋條件（只是過濾選項）
        // status 可作為獨立搜尋條件，支援只搜尋有效或作廢的付款單
        boolean empty =
                isEmpty(req.getSupplierName()) &&
                        isEmpty(req.getItem()) &&
                        isEmpty(req.getMethod()) &&
                        isEmpty(req.getAccountingPeriod()) &&
                        isEmpty(req.getFromDate()) &&
                        isEmpty(req.getToDate()) &&
                        isEmpty(req.getStatus());

        if (empty) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜尋條件不可全為空，至少需提供一項搜尋欄位（包含狀態過濾）"
            );
        }

        // ===== 2. 建 Specification =====
        Specification<Payment> spec = PaymentSpecifications.build(req);

        Page<Payment> result;

        try {
            result = paymentRepository.findAll(spec, pageable);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效排序欄位：" + ex.getPropertyName()
            );
        }

        // ===== 3. 轉 DTO =====
        return result.map(paymentMapper::toDto);
    }

    // =======================================================
    // ✅ 付款匯出（支援 format/scope；與 searchPayments 相同條件）
    // =======================================================
    @Override
    @Transactional(readOnly = true)
    public ExportPayload exportPayments(
            PaymentSearchRequest req,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    ) {
        PaymentSearchRequest request = req == null ? new PaymentSearchRequest() : req;

        ExportFormat safeFormat = format == null ? ExportFormat.XLSX : format;
        ExportScope safeScope = scope == null ? ExportScope.ALL : scope;

        Specification<Payment> spec = PaymentSpecifications.build(request);

        Sort safeSort = pageable != null && pageable.getSort() != null && pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.ASC, "id");

        List<String[]> rows;

        if (safeScope == ExportScope.ALL) {
            long total = paymentRepository.count(spec);
            if (total > maxExportRows) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "匯出筆數超過上限 (" + maxExportRows + ")，請縮小篩選條件");
            }
            rows = new java.util.ArrayList<>((int) Math.min(total, Integer.MAX_VALUE));

            int step = 1000;
            if (pageable != null && pageable.getPageSize() > 0 && pageable.getPageSize() <= 200) {
                step = Math.max(50, pageable.getPageSize());
            }

            int pages = (int) ((total + step - 1) / step);
            try {
                for (int p = 0; p < pages; p++) {
                    Page<Payment> page = paymentRepository.findAll(spec, PageRequest.of(p, step, safeSort));
                    for (Payment payment : page.getContent()) {
                        rows.add(toPaymentExportRow(paymentMapper.toDto(payment)));
                    }
                }
            } catch (PropertyReferenceException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "無效排序欄位：" + ex.getPropertyName());
            }
        } else {
            rows = new java.util.ArrayList<>();
            Pageable p = pageable == null
                    ? PageRequest.of(0, 25, safeSort)
                    : normalizeForExport(pageable, safeSort);
            try {
                Page<Payment> page = paymentRepository.findAll(spec, p);
                for (Payment payment : page.getContent()) {
                    rows.add(toPaymentExportRow(paymentMapper.toDto(payment)));
                }
            } catch (PropertyReferenceException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "無效排序欄位：" + ex.getPropertyName());
            }
        }

        byte[] data = switch (safeFormat) {
            case XLSX -> TabularExporter.toXlsx("payments", PAYMENT_EXPORT_HEADERS, rows);
            case CSV -> TabularExporter.toCsvUtf8Bom(PAYMENT_EXPORT_HEADERS, rows);
        };

        String filename = ExportFilenameUtils.build("payments", safeFormat);
        return new ExportPayload(data, filename, safeFormat.mediaType());
    }

    private Pageable normalizeForExport(Pageable pageable, Sort safeSort) {
        if (pageable.getPageNumber() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 無效");
        }
        int size = pageable.getPageSize();
        if (size <= 0 || size > 200) size = 25;
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }

    private static String[] toPaymentExportRow(PaymentResponseDto p) {
        return new String[]{
                nz(p.getPurchaseNo()),
                nz(p.getSupplierName()),
                nz(p.getItem()),
                nz(p.getMethod()),
                p.getAmount() == null ? "" : p.getAmount().toPlainString(),
                p.getPayDate() == null ? "" : p.getPayDate().toString(),
                nz(p.getAccountingPeriod()),
                nz(p.getStatus()),
                nz(p.getNote())
        };
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /* =======================================================
     * 📌 作廢付款單
     * ======================================================= */
    @Override
    @Transactional
    public PaymentResponseDto voidPayment(Long id, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到付款 ID：" + id));

        // 檢查是否已作廢
        if (payment.getStatus() == PaymentRecordStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "此付款單已經作廢");
        }

        // ⭐ 任何狀態都可以作廢（不需要檢查付款狀態）
        payment.setStatus(PaymentRecordStatus.VOIDED);
        payment.setVoidedAt(LocalDateTime.now());
        payment.setVoidReason(reason);

        paymentRepository.save(payment);

        // ⭐ 重新計算關聯進貨單的付款狀態（自動排除已作廢的付款）
        Purchase purchase = payment.getPurchase();
        recalcPaymentStatus(purchase);

        log.info("✅ 作廢付款：paymentId={}, purchaseId={}, reason={}",
                id, purchase.getId(), reason);
        
        // ⭐ 重新查詢以確保關聯資料被載入（用於映射 purchaseNo 和 supplierName）
        Payment savedPayment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到付款 ID：" + id));
        
        // 返回更新後的付款單 DTO（滿足 React Admin 的要求）
        return paymentMapper.toDto(savedPayment);
    }

    /* =======================================================
     * ⭐ 核心：重算進貨單的付款狀態
     * ======================================================= */
    /**
     * 計算進貨單的付款狀態，包含以下邏輯：
     * 1. 計算有效付款金額（排除已作廢的付款）
     * 2. 如果進貨單已作廢，只更新 paidAmount 和 balance，不更新 status（保留作廢前的狀態）
     * 3. 如果進貨單未作廢，根據已付款金額與總金額比較，決定狀態：PENDING / PARTIAL / PAID
     * 4. 如果曾經有付款記錄（包括已作廢的），即使現在有效付款為0，也保持 PAID 狀態
     */
    private void recalcPaymentStatus(Purchase purchase) {
        BigDecimal paidAmount = paymentRepository.sumAmountByPurchaseId(purchase.getId(), PaymentRecordStatus.ACTIVE);
        
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
        
        BigDecimal totalAmount = purchase.getTotalAmount();
        
        // ⭐ 更新已付款金額和餘額（無論是否已作廢都需要更新）
        purchase.setPaidAmount(paidAmount);
        purchase.setBalance(totalAmount.subtract(paidAmount));
        
        // ⭐ 如果進貨單已作廢，不更新 status（保留作廢前的付款狀態）
        if (purchase.getRecordStatus() == PurchaseStatus.VOIDED) {
            purchaseRepository.save(purchase);
            log.debug("進貨單已作廢，僅更新付款金額，保留原有狀態：purchaseId={}, status={}", 
                    purchase.getId(), purchase.getStatus());
            return;
        }
        
        // ⭐ 如果進貨單未作廢，重新計算付款狀態
        // 如果進貨單曾經有付款記錄（包括已作廢的），即使現在有效付款為0，也應該保持 PAID 狀態
        // 這樣可以防止已付款的進貨單在付款單被作廢後變成 PENDING，從而避免前端顯示錯誤
        boolean hasAnyPayment = paymentRepository.hasAnyPaymentByPurchaseId(purchase.getId());
        
        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            // 如果曾經有付款記錄，即使現在都被作廢了，也應該保持 PAID 狀態
            if (hasAnyPayment) {
                purchase.setStatus(Purchase.Status.PAID);
            } else {
                purchase.setStatus(Purchase.Status.PENDING);
            }
        } else if (paidAmount.compareTo(totalAmount) < 0) {
            purchase.setStatus(Purchase.Status.PARTIAL);
        } else {
            purchase.setStatus(Purchase.Status.PAID);
        }
        
        purchaseRepository.save(purchase);
    }

    /* =======================================================
     * 📌 工具方法
     * ======================================================= */
    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
