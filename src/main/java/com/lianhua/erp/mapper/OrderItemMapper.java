package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.OrderItem;
import com.lianhua.erp.dto.orderItem.OrderItemRequestDto;
import com.lianhua.erp.dto.orderItem.OrderItemResponseDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    OrderItem toEntity(OrderItemRequestDto dto);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    OrderItemResponseDto toResponseDto(OrderItem entity);
}
