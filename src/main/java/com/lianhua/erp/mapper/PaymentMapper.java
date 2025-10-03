package com.lianhua.erp.mapper;

import com.lianhua.erp.dto.PaymentDto;
import com.lianhua.erp.domin.Payment;
import com.lianhua.erp.domin.Purchase;
import com.lianhua.erp.repository.PurchaseRepository;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    private final PurchaseRepository purchaseRepository;

    public PaymentMapper(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public PaymentDto toDto(Payment payment) {
        if (payment == null) return null;

        return PaymentDto.builder()
                .id(payment.getId())
                .purchaseId(payment.getPurchase() != null ? payment.getPurchase().getId() : null)
                .amount(payment.getAmount())
                .payDate(payment.getPayDate() != null ? payment.getPayDate().toString() : null)
                .method(payment.getMethod() != null ? payment.getMethod().name() : null)
                .note(payment.getNote())
                .build();
    }

    public Payment toEntity(PaymentDto dto) {
        if (dto == null) return null;

        Payment payment = new Payment();
        payment.setId(dto.getId());
        payment.setAmount(dto.getAmount());

        if (dto.getPayDate() != null) {
            payment.setPayDate(java.time.LocalDate.parse(dto.getPayDate()));
        }

        if (dto.getMethod() != null) {
            payment.setMethod(Payment.Method.valueOf(dto.getMethod()));
        }

        payment.setNote(dto.getNote());

        if (dto.getPurchaseId() != null) {
            Purchase purchase = purchaseRepository.findById(dto.getPurchaseId())
                    .orElseThrow(() -> new RuntimeException("Purchase not found: " + dto.getPurchaseId()));
            payment.setPurchase(purchase);
        }

        return payment;
    }
}

