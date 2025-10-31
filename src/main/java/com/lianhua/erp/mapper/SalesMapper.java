package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.Sale;
import com.lianhua.erp.dto.sale.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SalesMapper {
    
    @Mapping(target = "product.id", source = "productId")
    @Mapping(target = "payMethod", expression = "java(Sale.PayMethod.valueOf(dto.getPayMethod()))")
    Sale toEntity(SalesRequestDto dto);
    
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "payMethod", expression = "java(sale.getPayMethod().name())")
    SalesResponseDto toDto(Sale sale);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(SalesRequestDto dto, @MappingTarget Sale sale);
}
