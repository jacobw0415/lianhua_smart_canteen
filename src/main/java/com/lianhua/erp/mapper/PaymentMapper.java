package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.Payment;
import com.lianhua.erp.dto.payment.PaymentDto;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    
    // === Entity → DTO ===
    @Mapping(target = "method", expression = "java(payment.getMethod().name())")
    PaymentDto toDto(Payment payment);
    
    // === DTO → Entity ===
    @Mapping(target = "id", ignore = true) // ✅ 永遠忽略 ID，防止 detached entity
    @Mapping(target = "method", expression = "java(mapMethod(dto.getMethod()))")
    @Mapping(target = "purchase", ignore = true) // 由 Service 層設定
    Payment toEntity(PaymentDto dto);
    
    List<PaymentDto> toDtoList(List<Payment> payments);
    List<Payment> toEntityList(List<PaymentDto> dtos);
    
    default Payment.Method mapMethod(String method) {
        if (method == null) return Payment.Method.CASH;
        try {
            return Payment.Method.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Payment.Method.CASH;
        }
    }
}
