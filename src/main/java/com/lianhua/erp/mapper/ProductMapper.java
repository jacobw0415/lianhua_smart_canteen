package com.lianhua.erp.mapper;

import com.lianhua.erp.dto.ProductDto;
import com.lianhua.erp.domin.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductDto toDto(Product product) {
        if (product == null) return null;

        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .category(String.valueOf(product.getCategory()))
                .unitPrice(product.getUnitPrice())   // 對應 entity 的 unitPrice
                .active(product.getActive())         // 對應 entity 的 active
                .build();
    }

    public Product toEntity(ProductDto dto) {
        if (dto == null) return null;

        return Product.builder()
                .id(dto.getId())
                .name(dto.getName())
                .category(Product.Category.valueOf(dto.getCategory()))
                .unitPrice(dto.getUnitPrice())       // 對應 dto 的 unitPrice
                .active(dto.getActive())             // 對應 dto 的 active
                .build();
    }

    public void updateEntityFromDto(ProductDto dto, Product product) {
        if (dto == null || product == null) return;

        product.setName(dto.getName());
        product.setCategory(Product.Category.valueOf(dto.getCategory()));
        product.setUnitPrice(dto.getUnitPrice());
        product.setActive(dto.getActive());
    }
}
