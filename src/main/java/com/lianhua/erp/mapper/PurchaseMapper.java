package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.Payment;
import com.lianhua.erp.domin.Purchase;
import com.lianhua.erp.dto.purchase.PurchaseRequestDto;
import com.lianhua.erp.dto.purchase.PurchaseResponseDto;
import org.mapstruct.*;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", uses = {PaymentMapper.class})
public interface PurchaseMapper {
    
    // DTO → Entity
    @Mapping(source = "supplierId", target = "supplier.id")
    @Mapping(target = "payments", ignore = true)
    Purchase toEntity(PurchaseRequestDto dto);
    
    // Entity → DTO（包含 supplierName + payments）
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(source = "payments", target = "payments")
    @Mapping(target = "totalAmount", expression = "java(calcTotal(entity))")
    @Mapping(target = "taxAmount", expression = "java(calcTaxAmount(entity))")
    PurchaseResponseDto toDto(Purchase entity);
    
    // 更新現有 entity（用於 PUT / PATCH）
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(PurchaseRequestDto dto, @MappingTarget Purchase entity);
    
    // 計算總金額（含稅）
    default BigDecimal calcTotal(Purchase p) {
        if (p == null || p.getUnitPrice() == null || p.getQty() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal subtotal = p.getUnitPrice().multiply(BigDecimal.valueOf(p.getQty()));
        return subtotal.add(calcTaxAmount(p));
    }
    
    // 計算稅額
    default BigDecimal calcTaxAmount(Purchase p) {
        if (p == null || p.getUnitPrice() == null || p.getQty() == null || p.getTaxRate() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal subtotal = p.getUnitPrice().multiply(BigDecimal.valueOf(p.getQty()));
        return subtotal.multiply(p.getTaxRate().divide(BigDecimal.valueOf(100)));
    }
    
    // 計算已付金額
    default BigDecimal calcPaid(Purchase p) {
        if (p == null || p.getPayments() == null) {
            return BigDecimal.ZERO;
        }
        return p.getPayments().stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
