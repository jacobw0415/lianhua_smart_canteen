package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.Order;
import com.lianhua.erp.domain.OrderItem;
import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.dto.orderItem.OrderItemResponseDto;
import org.mapstruct.*;


@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    // ================================
    // DTO → Entity（建立訂單）
    // ================================
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "recordStatus", ignore = true)
    @Mapping(target = "voidedAt", ignore = true)
    @Mapping(target = "voidReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(OrderRequestDto dto);

    // ================================
    // DTO → Entity（更新訂單）
    // ================================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "customer", ignore = true),
            @Mapping(target = "items", ignore = true),
            @Mapping(target = "paymentStatus", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
    })
    void updateEntityFromDto(OrderRequestDto dto, @MappingTarget Order entity);

    // ================================
    // OrderItem → Response DTO
    // ================================
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "qty", source = "qty")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    OrderItemResponseDto toResponseDto(OrderItem entity);

    // ================================
    // Entity → DTO（含 items，Service 用）
    // ================================
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(
            target = "items",
            expression =
                    "java(order.getItems() != null " +
                            "? order.getItems().stream().map(orderItemMapper::toResponseDto).toList() " +
                            ": java.util.Collections.emptyList())"
    )
    OrderResponseDto toResponseDto(Order order, @Context OrderItemMapper orderItemMapper);

    // ================================
    // Entity → DTO（無 context 相容版）
    // ================================
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "items", ignore = true)
    OrderResponseDto toResponseDto(Order order);
}
