package com.lianhua.erp.service;

import com.lianhua.erp.dto.supplier.SupplierDto;
import java.util.List;

public interface SupplierService {
    List<SupplierDto> getAllSuppliers();
    SupplierDto getSupplierById(Long id);
    SupplierDto createSupplier(SupplierDto dto);
    SupplierDto updateSupplier(Long id, SupplierDto dto);
    void deleteSupplier(Long id);
}
