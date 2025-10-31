package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.Payment;
import com.lianhua.erp.dto.payment.PaymentRequestDto;
import com.lianhua.erp.dto.payment.PaymentResponseDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    
    @Mapping(source = "purchase.id", target = "purchaseId")
    PaymentResponseDto toDto(Payment entity);
    
    @Mapping(target = "id", ignore = true)  // Ignore ID as it will be generated
    @Mapping(target = "purchase", ignore = true)  // Will be set manually
    Payment toEntity(PaymentRequestDto dto);
    
    default Payment.Method mapMethod(String method) {
        if (method == null) return Payment.Method.CASH;
        try {
            return Payment.Method.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Payment.Method.CASH;
        }
    }
}

