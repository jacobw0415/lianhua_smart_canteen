package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.Order;
import com.lianhua.erp.domain.OrderItem;
import com.lianhua.erp.dto.order.*;
import com.lianhua.erp.dto.orderItem.OrderItemResponseDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {
    
    // ================================
    // DTO → Entity
    // ================================
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "items", ignore = true)
    Order toEntity(OrderRequestDto dto);
    
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "qty", source = "qty")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    OrderItemResponseDto toResponseDto(OrderItem entity);
    // ================================
    // Entity → DTO（Service 呼叫用版本）
    // ================================
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(
            target = "items",
            expression = "java(order.getItems() != null ? order.getItems().stream().map(orderItemMapper::toResponseDto).toList() : java.util.Collections.emptyList())"
    )
    OrderResponseDto toResponseDto(Order order, @Context OrderItemMapper orderItemMapper);
    
    // ================================
    // Entity → DTO（無 context 時兼容版本）
    // ================================
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "items", ignore = true)
    OrderResponseDto toResponseDto(Order order);
}
