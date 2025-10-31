package com.lianhua.erp.service;

import com.lianhua.erp.dto.product.ProductCategoryRequestDto;
import com.lianhua.erp.dto.product.ProductCategoryResponseDto;
import java.util.List;

public interface ProductCategoryService {

    ProductCategoryResponseDto create(ProductCategoryRequestDto dto);

    ProductCategoryResponseDto update(Long id, ProductCategoryRequestDto dto);

    ProductCategoryResponseDto getById(Long id);

    List<ProductCategoryResponseDto> getAll();

    List<ProductCategoryResponseDto> getActive();

    void delete(Long id);
}
