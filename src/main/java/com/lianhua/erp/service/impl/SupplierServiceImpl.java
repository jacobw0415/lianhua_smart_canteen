package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.SupplierDto;
import com.lianhua.erp.domin.Supplier;
import com.lianhua.erp.mapper.SupplierMapper;
import com.lianhua.erp.repository.SupplierRepository;
import com.lianhua.erp.service.SupplierService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    public SupplierServiceImpl(SupplierRepository supplierRepository, SupplierMapper supplierMapper) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
    }

    @Override
    public List<SupplierDto> getAllSuppliers() {
        return supplierRepository.findAll()
                .stream()
                .map(supplierMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public SupplierDto getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found: " + id));
        return supplierMapper.toDto(supplier);
    }

    @Override
    public SupplierDto createSupplier(SupplierDto dto) {
        Supplier supplier = supplierMapper.toEntity(dto);
        return supplierMapper.toDto(supplierRepository.save(supplier));
    }

    @Override
    public SupplierDto updateSupplier(Long id, SupplierDto dto) {
        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found: " + id));

        existing.setName(dto.getName());
        existing.setContact(dto.getContact());
        existing.setPhone(dto.getPhone());

        if (dto.getBillingCycle() != null) {
            existing.setBillingCycle(Supplier.BillingCycle.valueOf(dto.getBillingCycle()));
        }

        existing.setNote(dto.getNote());
        return supplierMapper.toDto(supplierRepository.save(existing));
    }

    @Override
    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }
}
