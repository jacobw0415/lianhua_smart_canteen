package com.lianhua.erp.service;

import com.lianhua.erp.dto.product.*;
import java.util.List;

public interface ProductService {

    ProductResponseDto create(ProductRequestDto dto);
    ProductResponseDto update(Long id, ProductRequestDto dto);
    ProductResponseDto getById(Long id);
    List<ProductResponseDto> getAll();
    List<ProductResponseDto> getActiveProducts();
    ProductResponseDto getWithRelations(Long id);
    void delete(Long id);
}
