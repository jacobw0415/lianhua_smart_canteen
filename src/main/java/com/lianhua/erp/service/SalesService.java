package com.lianhua.erp.service;

import com.lianhua.erp.dto.sale.*;

import java.util.List;

public interface SalesService {
    
    SalesResponseDto create(SalesRequestDto dto);
    
    SalesResponseDto update(Long id, SalesRequestDto dto);
    
    void delete(Long id);
    
    SalesResponseDto findById(Long id);
    
    List<SalesResponseDto> findAll();
    
    List<SalesResponseDto> findByProduct(Long productId);
}
