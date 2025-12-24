package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Payment;
import com.lianhua.erp.dto.payment.PaymentResponseDto;
import com.lianhua.erp.dto.payment.PaymentSearchRequest;
import com.lianhua.erp.mapper.PaymentMapper;
import com.lianhua.erp.repository.PaymentRepository;
import com.lianhua.erp.service.PaymentService;
import com.lianhua.erp.service.impl.spec.PaymentSpecifications;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

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
        boolean empty =
                isEmpty(req.getSupplierName()) &&
                        isEmpty(req.getItem()) &&
                        isEmpty(req.getMethod()) &&
                        isEmpty(req.getAccountingPeriod()) &&
                        isEmpty(req.getFromDate()) &&
                        isEmpty(req.getToDate());

        if (empty) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜尋條件不可全為空，至少需提供一項搜尋欄位"
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

    /* =======================================================
     * 📌 工具方法
     * ======================================================= */
    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
