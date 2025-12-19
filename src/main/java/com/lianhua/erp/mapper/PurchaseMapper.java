package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.Payment;
import com.lianhua.erp.domain.Purchase;
import com.lianhua.erp.dto.purchase.PurchaseRequestDto;
import com.lianhua.erp.dto.purchase.PurchaseResponseDto;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@Mapper(componentModel = "spring", uses = {PaymentMapper.class})
public interface PurchaseMapper {
    
    @Mapping(source = "supplierId", target = "supplier.id")
    @Mapping(target = "payments", ignore = true)
    Purchase toEntity(PurchaseRequestDto dto);
    @Mapping(source = "purchaseNo", target = "purchaseNo")
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(source = "unit", target = "unit")
    @Mapping(source = "payments", target = "payments")
    @Mapping(target = "totalAmount", expression = "java(calcTotal(entity))")
    @Mapping(target = "paidAmount", expression = "java(calcPaid(entity))")
    @Mapping(target = "balance", expression = "java(calcTotal(entity).subtract(calcPaid(entity)).setScale(2, java.math.RoundingMode.HALF_UP))")
    PurchaseResponseDto toDto(Purchase entity);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(PurchaseRequestDto dto, @MappingTarget Purchase entity);
    
    // === 金額運算 ===
    default BigDecimal calcTotal(Purchase p) {
        if (p == null || p.getUnitPrice() == null || p.getQty() == null) return BigDecimal.ZERO;
        BigDecimal subtotal = p.getUnitPrice().multiply(BigDecimal.valueOf(p.getQty()));
        return subtotal.add(calcTaxAmount(p)).setScale(2, RoundingMode.HALF_UP);
    }
    
    default BigDecimal calcTaxAmount(Purchase p) {
        if (p == null || p.getTaxRate() == null || p.getQty() == null || p.getUnitPrice() == null)
            return BigDecimal.ZERO;
        BigDecimal subtotal = p.getUnitPrice().multiply(BigDecimal.valueOf(p.getQty()));
        return subtotal.multiply(p.getTaxRate()
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    default BigDecimal calcPaid(Purchase p) {
        return Optional.ofNullable(p.getPayments())
                .orElse(Collections.emptySet())
                .stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    default PurchaseResponseDto toResponseDto(Purchase entity) {
        return toDto(entity);
    }
}
