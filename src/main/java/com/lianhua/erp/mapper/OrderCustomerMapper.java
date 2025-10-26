package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.OrderCustomer;
import com.lianhua.erp.dto.orderCustomer.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrderCustomerMapper {
    
    OrderCustomer toEntity(OrderCustomerRequestDto dto);
    
    @Mapping(target = "billingCycle", expression = "java(entity.getBillingCycle().name())")
    OrderCustomerResponseDto toResponseDto(OrderCustomer entity);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(OrderCustomerRequestDto dto, @MappingTarget OrderCustomer entity);
}
