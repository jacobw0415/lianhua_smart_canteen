package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Payment;
import com.lianhua.erp.dto.payment.PaymentResponseDto;
import com.lianhua.erp.mapper.PaymentMapper;
import com.lianhua.erp.repository.PaymentRepository;
import com.lianhua.erp.service.PaymentService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .map(paymentMapper::toDto);   // Page.map 自動保留 totalElements
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
}
