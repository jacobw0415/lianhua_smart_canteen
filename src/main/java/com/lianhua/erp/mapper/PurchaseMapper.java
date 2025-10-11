package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.*;
import com.lianhua.erp.dto.payment.*;
import com.lianhua.erp.dto.purchase.*;
import org.mapstruct.*;
import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", uses = {PaymentMapper.class})
public interface PurchaseMapper {
    
    // === Entity → DTO ===
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "status", expression = "java(purchase.getStatus().name())")
    @Mapping(target = "totalAmount", expression = "java(calcTotal(purchase))")
    @Mapping(target = "paidAmount", expression = "java(calcPaid(purchase))")
    @Mapping(target = "balance", expression = "java(calcBalance(purchase))")
    PurchaseDto toDto(Purchase purchase);
    
    // === Request → Entity ===
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "status", expression = "java(mapStatus(dto.getStatus()))")
    Purchase toEntity(PurchaseRequestDto dto);
    
    // === ENUM 轉換 ===
    default Purchase.Status mapStatus(String status) {
        if (status == null) return Purchase.Status.PENDING;
        try {
            return Purchase.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Purchase.Status.PENDING;
        }
    }
    
    // === 計算輔助 ===
    default BigDecimal calcTotal(Purchase p) {
        if (p == null || p.getUnitPrice() == null || p.getQty() == null) return BigDecimal.ZERO;
        BigDecimal total = p.getUnitPrice().multiply(BigDecimal.valueOf(p.getQty()));
        if (p.getTax() != null) total = total.add(p.getTax());
        return total;
    }
    
    default BigDecimal calcPaid(Purchase p) {
        if (p == null || p.getPayments() == null) return BigDecimal.ZERO;
        return p.getPayments().stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    default BigDecimal calcBalance(Purchase p) {
        return calcTotal(p).subtract(calcPaid(p));
    }
}
