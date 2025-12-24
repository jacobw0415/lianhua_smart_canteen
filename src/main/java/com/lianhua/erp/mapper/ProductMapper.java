package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.OrderItem;
import com.lianhua.erp.domain.Product;
import com.lianhua.erp.domain.ProductCategory;
import com.lianhua.erp.domain.Sale;
import com.lianhua.erp.dto.product.ProductRequestDto;
import com.lianhua.erp.dto.product.ProductResponseDto;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "category", expression = "java(fromCategoryId(dto.getCategoryId()))")
    Product toEntity(ProductRequestDto dto);

    @Mapping(target = "category.id", source = "category.id")
    @Mapping(target = "category.name", source = "category.name")
    @Mapping(target = "category.code", source = "category.code")
    @Mapping(target = "saleIds", expression = "java(mapSaleIds(entity))")
    @Mapping(target = "orderItemIds", expression = "java(mapOrderItemIds(entity))")
    ProductResponseDto toDto(Product entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "name", ignore = true),  // 名稱由 Service 層手動處理（包含 trim 和唯一性檢查）
            @Mapping(target = "category", ignore = true),  // 分類由 Service 層手動處理（包含存在性檢查）
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "sales", ignore = true),
            @Mapping(target = "orderItems", ignore = true)
    })
    void updateEntityFromDto(ProductRequestDto dto, @MappingTarget Product entity);

    default List<Long> mapSaleIds(Product entity) {
        if (entity.getSales() == null) return Collections.emptyList();
        return entity.getSales().stream().map(Sale::getId).collect(Collectors.toList());
    }

    default List<Long> mapOrderItemIds(Product entity) {
        if (entity.getOrderItems() == null) return Collections.emptyList();
        return entity.getOrderItems().stream().map(OrderItem::getId).collect(Collectors.toList());
    }

    default ProductCategory fromCategoryId(Long categoryId) {
        if (categoryId == null) return null;
        ProductCategory category = new ProductCategory();
        category.setId(categoryId);
        return category;
    }
}
