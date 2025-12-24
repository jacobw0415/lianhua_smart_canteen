package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.Receipt;
import com.lianhua.erp.domain.ReceiptStatus;
import com.lianhua.erp.dto.receipt.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ReceiptMapper {

    /**
     * 建立收款時使用
     *
     * 規則：
     * - order 由 Service 設定
     * - accountingPeriod 由 Service 計算
     * - amount 由 DTO 傳入（支援階段性付款）
     */
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "accountingPeriod", ignore = true)
    Receipt toEntity(ReceiptRequestDto dto);

    /**
     * Entity → Response DTO
     */
    @Mappings({
            @Mapping(source = "order.id", target = "orderId"),
            @Mapping(source = "order.orderNo", target = "orderNo"),
            @Mapping(source = "order.customer.name", target = "customerName"),
            @Mapping(target = "status", expression = "java(mapStatus(entity.getStatus()))"),
            @Mapping(source = "voidedAt", target = "voidedAt"),
            @Mapping(source = "voidReason", target = "voidReason")
    })
    ReceiptResponseDto toDto(Receipt entity);
    
    /**
     * ReceiptStatus enum → String
     */
    default String mapStatus(ReceiptStatus status) {
        return status != null ? status.name() : null;
    }

    /**
     * 更新收款時使用（部分更新）
     *
     * 規則：
     * - null 不覆蓋
     * - order 不可改
     * - accountingPeriod 由 Service 依 receivedDate 重算
     * - amount 允許修改（修正輸入錯誤）
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "order", ignore = true),
            @Mapping(target = "accountingPeriod", ignore = true)
    })
    void updateEntityFromDto(ReceiptRequestDto dto, @MappingTarget Receipt entity);
}
