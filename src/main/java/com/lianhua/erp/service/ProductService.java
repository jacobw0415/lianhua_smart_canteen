package com.lianhua.erp.service;

import com.lianhua.erp.dto.product.*;
import java.util.List;

public interface ProductService {

    ProductResponseDto create(ProductRequestDto dto);
    ProductResponseDto update(Long id, ProductRequestDto dto);
    ProductResponseDto getById(Long id);
    ProductResponseDto deactivate(Long id);
    ProductResponseDto activate(Long id);
    List<ProductResponseDto> getAll();
    List<ProductResponseDto> getActiveProducts();
    ProductResponseDto getWithRelations(Long id);
    List<ProductResponseDto> search(ProductSearchRequest search);
    void delete(Long id);
    List<ProductResponseDto> getByCategory(Long categoryId);
}
