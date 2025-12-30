package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.Payment;
import com.lianhua.erp.domain.PaymentRecordStatus;
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
            
            @Mapping(source = "purchase.purchaseNo", target = "purchaseNo"),
            
            // 新增：供應商名稱
            @Mapping(source = "purchase.supplier.name", target = "supplierName"),

            // 新增：品項摘要（從明細表取得第一個品項）
            @Mapping(target = "item", expression = "java(getFirstItemName(entity.getPurchase()))"),

            // 新增：會計期間
            @Mapping(source = "accountingPeriod", target = "accountingPeriod"),

            // 新增：作廢相關欄位
            @Mapping(target = "status", expression = "java(mapStatus(entity.getStatus()))"),
            @Mapping(source = "voidedAt", target = "voidedAt"),
            @Mapping(source = "voidReason", target = "voidReason")
    })
    PaymentResponseDto toDto(Payment entity);

    /**
     * PaymentRecordStatus enum → String
     */
    default String mapStatus(PaymentRecordStatus status) {
        return status != null ? status.name() : null;
    }


    /* ============================================
     * 📌 PaymentRequestDto → Payment（新增付款時使用）
     * ============================================ */
    @Mappings({
            @Mapping(target = "id", ignore = true),          // ID 自動生成
            @Mapping(target = "purchase", ignore = true),    // 由 Service 手動設定
            @Mapping(target = "method", expression = "java(mapMethod(dto.getMethod()))"),
            @Mapping(target = "accountingPeriod", ignore = true),  // 由 Service 設定
            @Mapping(target = "referenceNo", ignore = true),       // 由 Service 設定
            @Mapping(target = "status", ignore = true),           // 預設值
            @Mapping(target = "voidedAt", ignore = true),
            @Mapping(target = "voidReason", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
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
    
    default String getFirstItemName(com.lianhua.erp.domain.Purchase purchase) {
        if (purchase == null || purchase.getItems() == null || purchase.getItems().isEmpty()) {
            return null;
        }
        return purchase.getItems().get(0).getItem();
    }
}
