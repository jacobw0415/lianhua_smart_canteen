package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.Payment;
import com.lianhua.erp.dto.payment.PaymentRequestDto;
import com.lianhua.erp.dto.payment.PaymentResponseDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    /* ============================================
     * 📌 Payment → PaymentResponseDto
     * ============================================ */
    @Mappings({
            @Mapping(source = "purchase.id", target = "purchaseId"),

            // 新增：供應商名稱
            @Mapping(source = "purchase.supplier.name", target = "supplierName"),

            // 新增：品項摘要
            @Mapping(source = "purchase.item", target = "item"),

            // 新增：會計期間
            @Mapping(source = "accountingPeriod", target = "accountingPeriod")
    })
    PaymentResponseDto toDto(Payment entity);


    /* ============================================
     * 📌 PaymentRequestDto → Payment（新增付款時使用）
     * ============================================ */
    @Mappings({
            @Mapping(target = "id", ignore = true),          // ID 自動生成
            @Mapping(target = "purchase", ignore = true),    // 由 Service 手動設定
            @Mapping(target = "method", expression = "java(mapMethod(dto.getMethod()))")
    })
    Payment toEntity(PaymentRequestDto dto);


    /* ============================================
     * 📌 付款方式字串 → Enum
     * ============================================ */
    default Payment.Method mapMethod(String method) {
        if (method == null) return Payment.Method.CASH;
        try {
            return Payment.Method.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Payment.Method.CASH;
        }
    }
}
