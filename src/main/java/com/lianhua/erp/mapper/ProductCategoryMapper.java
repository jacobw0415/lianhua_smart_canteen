package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.ProductCategory;
import com.lianhua.erp.dto.product.ProductCategoryRequestDto;
import com.lianhua.erp.dto.product.ProductCategoryResponseDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductCategoryMapper {

    ProductCategory toEntity(ProductCategoryRequestDto dto);

    ProductCategoryResponseDto toDto(ProductCategory entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ProductCategoryRequestDto dto, @MappingTarget ProductCategory entity);
}
