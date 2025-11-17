package com.lianhua.erp.service;

import com.lianhua.erp.dto.supplier.SupplierDto;
import com.lianhua.erp.dto.supplier.SupplierRequestDto;
import com.lianhua.erp.dto.supplier.SupplierSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {

    Page<SupplierDto> getAllSuppliers(Pageable pageable);
    SupplierDto getSupplierById(Long id);
    SupplierDto createSupplier(SupplierRequestDto dto);
    SupplierDto updateSupplier(Long id, SupplierRequestDto dto);
    void deleteSupplier(Long id);
    Page<SupplierDto> searchSuppliers(SupplierSearchRequest request, Pageable pageable);
}
