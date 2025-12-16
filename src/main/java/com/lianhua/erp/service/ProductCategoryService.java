package com.lianhua.erp.service;

import com.lianhua.erp.dto.product.ProductCategoryRequestDto;
import com.lianhua.erp.dto.product.ProductCategoryResponseDto;
import com.lianhua.erp.dto.product.ProductCategorySearchRequest;

import java.util.List;

public interface ProductCategoryService {

    ProductCategoryResponseDto create(ProductCategoryRequestDto dto);

    ProductCategoryResponseDto update(Long id, ProductCategoryRequestDto dto);

    ProductCategoryResponseDto getById(Long id);

    List<ProductCategoryResponseDto> getAll();

    List<ProductCategoryResponseDto> getActive();

    ProductCategoryResponseDto deactivate(Long id);

    ProductCategoryResponseDto activate(Long id);

    List<ProductCategoryResponseDto> search(ProductCategorySearchRequest search);

    void delete(Long id);
}
