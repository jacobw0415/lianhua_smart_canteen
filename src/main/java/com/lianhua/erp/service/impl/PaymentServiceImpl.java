package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Payment;
import com.lianhua.erp.dto.payment.PaymentResponseDto;
import com.lianhua.erp.mapper.PaymentMapper;
import com.lianhua.erp.repository.PaymentRepository;
import com.lianhua.erp.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    
    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> findAll() {
        return paymentRepository.findAll().stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto findByPurchaseId(Long purchaseId) {
        List<Payment> payments = paymentRepository.findByPurchaseId(purchaseId);
        if (payments.isEmpty()) {
            throw new EntityNotFoundException("找不到指定進貨單的付款紀錄，purchaseId=" + purchaseId);
        }
        // Assuming you want to return the first payment for the purchase
        return paymentMapper.toDto(payments.get(0));
    }
    
    @Override
    @Transactional
    public void deleteByPurchaseId(Long purchaseId) {
        paymentRepository.deleteByPurchaseId(purchaseId);
    }
}