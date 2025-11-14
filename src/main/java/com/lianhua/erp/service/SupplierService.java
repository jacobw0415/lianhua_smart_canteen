package com.lianhua.erp.service;

import com.lianhua.erp.dto.supplier.SupplierDto;
import com.lianhua.erp.dto.supplier.SupplierRequestDto;
import com.lianhua.erp.dto.supplier.SupplierSearchRequest;

import java.util.List;

public interface SupplierService {
    List<SupplierDto> getAllSuppliers();
    SupplierDto getSupplierById(Long id);
    SupplierDto createSupplier(SupplierRequestDto dto);
    SupplierDto updateSupplier(Long id, SupplierRequestDto dto);
    void deleteSupplier(Long id);
    List<SupplierDto> searchSuppliers(SupplierSearchRequest request);

}
