package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.purchase.PurchaseDto;
import com.lianhua.erp.domin.Purchase;
import com.lianhua.erp.domin.Payment;
import com.lianhua.erp.mapper.PurchaseMapper;
import com.lianhua.erp.mapper.PaymentMapper;
import com.lianhua.erp.repository.PurchaseRepository;
import com.lianhua.erp.repository.PaymentRepository;
import com.lianhua.erp.service.PurchaseService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PaymentRepository paymentRepository;
    private final PurchaseMapper purchaseMapper;
    private final PaymentMapper paymentMapper;

    public PurchaseServiceImpl(PurchaseRepository purchaseRepository,
                               PaymentRepository paymentRepository,
                               PurchaseMapper purchaseMapper,
                               PaymentMapper paymentMapper) {
        this.purchaseRepository = purchaseRepository;
        this.paymentRepository = paymentRepository;
        this.purchaseMapper = purchaseMapper;
        this.paymentMapper = paymentMapper;
    }

    // === Purchase CRUD ===
    @Override
    public List<PurchaseDto> getAllPurchases() {
        return purchaseRepository.findAll().stream()
                .map(purchaseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PurchaseDto getPurchaseById(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Purchase not found: " + id));
        return purchaseMapper.toDto(purchase);
    }

    @Override
    public PurchaseDto createPurchase(PurchaseDto dto) {
        Purchase purchase = purchaseMapper.toEntity(dto);
        purchase = purchaseRepository.save(purchase);

        // 同時處理付款
        if (dto.getPayments() != null) {
            Purchase finalPurchase = purchase;
            List<Payment> payments = dto.getPayments().stream()
                    .map(paymentDto -> {
                        Payment payment = paymentMapper.toEntity(paymentDto);
                        payment.setPurchase(finalPurchase); // ✅ 直接設定，不用 peek
                        return payment;
                    })
                    .collect(Collectors.toList());

            paymentRepository.saveAll(payments);
            purchase.setPayments(payments);
        }

        return purchaseMapper.toDto(purchase);
    }

    @Override
    public PurchaseDto updatePurchase(Long id, PurchaseDto dto) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Purchase not found: " + id));

        purchaseMapper.updateEntityFromDto(dto, purchase);
        purchase = purchaseRepository.save(purchase);

        // 更新付款（這裡我用簡單策略：先刪掉舊的，再存新的）
        if (dto.getPayments() != null) {
            paymentRepository.deleteAll(purchase.getPayments());

            Purchase finalPurchase = purchase;
            List<Payment> payments = dto.getPayments().stream()
                    .map(paymentDto -> {
                        Payment payment = paymentMapper.toEntity(paymentDto);
                        payment.setPurchase(finalPurchase); // ✅ 一樣改成 map 處理
                        return payment;
                    })
                    .collect(Collectors.toList());

            paymentRepository.saveAll(payments);
            purchase.setPayments(payments);
        }

        return purchaseMapper.toDto(purchase);
    }


    @Override
    public void deletePurchase(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Purchase not found: " + id));

        // 先刪付款，避免外鍵衝突
        if (purchase.getPayments() != null && !purchase.getPayments().isEmpty()) {
            paymentRepository.deleteAll(purchase.getPayments());
        }

        purchaseRepository.delete(purchase);
    }
}
