package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.Product;
import com.lianhua.erp.domin.Sale;
import com.lianhua.erp.dto.sale.*;
import com.lianhua.erp.mapper.SalesMapper;
import com.lianhua.erp.repository.ProductRepository;
import com.lianhua.erp.repository.SalesRepository;
import com.lianhua.erp.service.SalesService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesServiceImpl implements SalesService {
    
    private final SalesRepository repository;
    private final ProductRepository productRepository;
    private final SalesMapper mapper;
    
    @Override
    public SalesResponseDto create(SalesRequestDto dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("找不到商品 ID: " + dto.getProductId()));
        
        // 檢查唯一約束 (同日期 + 同商品)
        if (repository.existsBySaleDateAndProductId(dto.getSaleDate(), dto.getProductId())) {
            throw new DataIntegrityViolationException("該商品於該日期已有銷售紀錄，請勿重複建立。");
        }
        
        Sale sale = mapper.toEntity(dto);
        sale.setProduct(product);
        return mapper.toDto(repository.save(sale));
    }
    
    @Override
    public SalesResponseDto update(Long id, SalesRequestDto dto) {
        Sale existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到銷售紀錄 ID: " + id));
        
        mapper.updateEntityFromDto(dto, existing);
        return mapper.toDto(repository.save(existing));
    }
    
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("找不到銷售紀錄 ID: " + id);
        }
        repository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public SalesResponseDto findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到銷售紀錄 ID: " + id));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SalesResponseDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SalesResponseDto> findByProduct(Long productId) {
        return repository.findByProductId(productId).stream().map(mapper::toDto).collect(Collectors.toList());
    }
}
