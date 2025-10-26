package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.Order;
import com.lianhua.erp.dto.order.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    
    @Mapping(target = "customer", ignore = true) // 手動處理在 ServiceImpl
    Order toEntity(OrderRequestDto dto);
    
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    OrderResponseDto toResponseDto(Order order);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(OrderRequestDto dto, @MappingTarget Order entity);
}
