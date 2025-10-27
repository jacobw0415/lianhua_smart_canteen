package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.Order;
import com.lianhua.erp.dto.order.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "items", ignore = true)
    Order toEntity(OrderRequestDto dto);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "items", expression = "java(order.getItems() != null ? order.getItems().stream().map(orderItemMapper::toResponseDto).toList() : null)")
    OrderResponseDto toResponseDto(Order order, @Context OrderItemMapper orderItemMapper);

    OrderResponseDto toResponseDto(Order order);
}
