package com.lianhua.erp.service;

import com.lianhua.erp.dto.SaleRequestDto;
import com.lianhua.erp.dto.SaleResponseDto;

import java.util.List;

public interface SaleService {
    List<SaleResponseDto> getAllSales();
    SaleResponseDto getSaleById(Long id);
    SaleResponseDto createSale(SaleRequestDto dto);
    SaleResponseDto updateSale(Long id, SaleRequestDto dto);
    void deleteSale(Long id);
}

