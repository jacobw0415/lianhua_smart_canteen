package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.Payment;
import com.lianhua.erp.domain.Purchase;
import com.lianhua.erp.domain.PurchaseItem;
import com.lianhua.erp.domain.PurchaseStatus;
import com.lianhua.erp.dto.purchase.PurchaseItemDto;
import com.lianhua.erp.dto.purchase.PurchaseRequestDto;
import com.lianhua.erp.dto.purchase.PurchaseResponseDto;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = { PaymentMapper.class, PurchaseItemMapper.class })
public interface PurchaseMapper {

    @Mappings({
            @Mapping(source = "supplierId", target = "supplier.id"),
            @Mapping(target = "payments", ignore = true),
            @Mapping(target = "items", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "purchaseNo", ignore = true),
            @Mapping(target = "accountingPeriod", ignore = true),
            @Mapping(target = "totalAmount", ignore = true),
            @Mapping(target = "paidAmount", ignore = true),
            @Mapping(target = "balance", ignore = true),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "recordStatus", ignore = true),
            @Mapping(target = "voidedAt", ignore = true),
            @Mapping(target = "voidReason", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
    })
    Purchase toEntity(PurchaseRequestDto dto);

    @Mapping(source = "purchaseNo", target = "purchaseNo")
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(source = "payments", target = "payments")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "paidAmount", expression = "java(calcPaid(entity))")
    @Mapping(target = "balance", expression = "java(entity.getTotalAmount().subtract(calcPaid(entity)).setScale(2, java.math.RoundingMode.HALF_UP))")
    @Mapping(target = "items", expression = "java(mapItems(entity.getItems()))")
    @Mapping(target = "recordStatus", expression = "java(mapRecordStatus(entity.getRecordStatus()))")
    @Mapping(source = "voidedAt", target = "voidedAt")
    @Mapping(source = "voidReason", target = "voidReason")
    PurchaseResponseDto toDto(Purchase entity, @Context PurchaseItemMapper purchaseItemMapper);

    default PurchaseResponseDto toDto(Purchase entity) {
        return toDto(entity, null);
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "purchaseNo", ignore = true),
            @Mapping(target = "supplier", ignore = true),
            @Mapping(target = "accountingPeriod", ignore = true),
            @Mapping(target = "totalAmount", ignore = true),
            @Mapping(target = "paidAmount", ignore = true),
            @Mapping(target = "balance", ignore = true),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "recordStatus", ignore = true),
            @Mapping(target = "voidedAt", ignore = true),
            @Mapping(target = "voidReason", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "payments", ignore = true),
            @Mapping(target = "items", ignore = true)
    })
    void updateEntityFromDto(PurchaseRequestDto dto, @MappingTarget Purchase entity);

    // === 金額運算 ===
    default BigDecimal calcPaid(Purchase p) {
        return Optional.ofNullable(p.getPayments())
                .orElse(Collections.emptySet())
                .stream()
                .filter(payment -> payment.getStatus() == com.lianhua.erp.domain.PaymentRecordStatus.ACTIVE)
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    default List<PurchaseItemDto> mapItems(List<PurchaseItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> {
                    PurchaseItemDto dto = new PurchaseItemDto();
                    dto.setId(item.getId());
                    dto.setItem(item.getItem());
                    dto.setUnit(item.getUnit());
                    dto.setQty(item.getQty());
                    dto.setUnitPrice(item.getUnitPrice());
                    dto.setSubtotal(item.getSubtotal());
                    dto.setNote(item.getNote());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    default PurchaseResponseDto toResponseDto(Purchase entity) {
        return toDto(entity);
    }

    /**
     * PurchaseStatus enum → String
     */
    default String mapRecordStatus(PurchaseStatus status) {
        return status != null ? status.name() : null;
    }
}
