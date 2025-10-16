package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.Supplier;
import com.lianhua.erp.dto.supplier.SupplierDto;
import com.lianhua.erp.dto.supplier.SupplierRequestDto;
import com.lianhua.erp.mapper.SupplierMapper;
import com.lianhua.erp.repository.SupplierRepository;
import com.lianhua.erp.service.SupplierService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {
    
    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    
    @Override
    @Transactional(readOnly = true)
    public List<SupplierDto> getAllSuppliers() {
        return supplierRepository.findAll()
                .stream()
                .map(supplierMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public SupplierDto getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到供應商 ID：" + id));
        return supplierMapper.toDto(supplier);
    }
    
    @Override
    public SupplierDto createSupplier(SupplierRequestDto dto) {
        if (supplierRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("供應商名稱已存在：" + dto.getName());
        }
        Supplier supplier = supplierMapper.toEntity(dto);
        return supplierMapper.toDto(supplierRepository.save(supplier));
    }

    @Override
    public SupplierDto updateSupplier(Long id, SupplierRequestDto dto) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到供應商 ID：" + id));

        if (!supplier.getName().equals(dto.getName())
                && supplierRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("供應商名稱已存在：" + dto.getName());
        }

        supplierMapper.updateEntityFromDto(dto, supplier);

        try {
            supplier = supplierRepository.save(supplier);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("更新供應商失敗，名稱可能已存在：" + dto.getName(), ex);
        }

        return supplierMapper.toDto(supplier);
    }
    
    @Override
    public void deleteSupplier(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new EntityNotFoundException("找不到供應商 ID：" + id);
        }
        supplierRepository.deleteById(id);
    }
}
