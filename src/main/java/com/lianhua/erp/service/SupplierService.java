package com.lianhua.erp.service;

import com.lianhua.erp.dto.supplier.SupplierDto;
import com.lianhua.erp.dto.supplier.SupplierRequestDto;
import com.lianhua.erp.dto.supplier.SupplierSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupplierService {

    Page<SupplierDto> getAllSuppliers(Pageable pageable);
    SupplierDto getSupplierById(Long id);
    SupplierDto createSupplier(SupplierRequestDto dto);
    SupplierDto updateSupplier(Long id, SupplierRequestDto dto);

    // ================================================================
    // 停用供應商（active = false）
    // ================================================================
    SupplierDto deactivateSupplier(Long id);

    // ================================================================
    // 啟用供應商（active = true）
    // ================================================================
    SupplierDto activateSupplier(Long id);

    void deleteSupplier(Long id);
    Page<SupplierDto> searchSuppliers(SupplierSearchRequest request, Pageable pageable);

    List<SupplierDto> getActiveSuppliers();

}
