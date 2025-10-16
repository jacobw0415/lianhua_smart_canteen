package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.Product;
import com.lianhua.erp.dto.product.ProductRequestDto;
import com.lianhua.erp.dto.product.ProductResponseDto;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "category", expression = "java(mapCategory(dto.getCategory()))")
    Product toEntity(ProductRequestDto dto);

    @Mapping(target = "category", expression = "java(entity.getCategory().name())")
    @Mapping(target = "saleIds", expression = "java(mapSaleIds(entity))")
    @Mapping(target = "orderItemIds", expression = "java(mapOrderItemIds(entity))")
    ProductResponseDto toDto(Product entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category", expression = "java(mapCategory(dto.getCategory()))")
    void updateEntityFromDto(ProductRequestDto dto, @MappingTarget Product entity);

    default Product.Category mapCategory(String category) {
        if (category == null) return null;
        return Product.Category.valueOf(category.toUpperCase());
    }

    default List<Long> mapSaleIds(Product entity) {
        if (entity.getSales() == null) return Collections.emptyList();
        return entity.getSales().stream().map(s -> s.getId()).collect(Collectors.toList());
    }

    default List<Long> mapOrderItemIds(Product entity) {
        if (entity.getOrderItems() == null) return Collections.emptyList();
        return entity.getOrderItems().stream().map(o -> o.getId()).collect(Collectors.toList());
    }
}
