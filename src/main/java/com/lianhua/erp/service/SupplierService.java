package com.lianhua.erp.service;

import com.lianhua.erp.dto.supplier.SupplierResponseDto;
import com.lianhua.erp.dto.supplier.SupplierRequestDto;
import com.lianhua.erp.dto.supplier.SupplierSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupplierService {

    Page<SupplierResponseDto> getAllSuppliers(Pageable pageable);
    SupplierResponseDto getSupplierById(Long id);
    SupplierResponseDto createSupplier(SupplierRequestDto dto);
    SupplierResponseDto updateSupplier(Long id, SupplierRequestDto dto);

    // ================================================================
    // 停用供應商（active = false）
    // ================================================================
    SupplierResponseDto deactivateSupplier(Long id);

    // ================================================================
    // 啟用供應商（active = true）
    // ================================================================
    SupplierResponseDto activateSupplier(Long id);

    void deleteSupplier(Long id);
    Page<SupplierResponseDto> searchSuppliers(SupplierSearchRequest request, Pageable pageable);

    List<SupplierResponseDto> getActiveSuppliers();

}
