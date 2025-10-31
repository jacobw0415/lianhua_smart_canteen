package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Product;
import com.lianhua.erp.dto.product.*;
import com.lianhua.erp.mapper.ProductMapper;
import com.lianhua.erp.repository.ProductRepository;
import com.lianhua.erp.service.ProductService;
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
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Override
    public ProductResponseDto create(ProductRequestDto dto) {
        if (repository.existsByName(dto.getName())) {
            throw new DataIntegrityViolationException("相同商品名稱已存在，請重新輸入商品名稱。");
        }
        Product product = mapper.toEntity(dto);
        return mapper.toDto(repository.save(product));
    }

    @Override
    public ProductResponseDto update(Long id, ProductRequestDto dto) {
        Product existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到商品 ID: " + id));

        // 若名稱改變，需檢查是否與他人重複
        if (!existing.getName().equalsIgnoreCase(dto.getName()) && repository.existsByName(dto.getName())) {
            throw new DataIntegrityViolationException("相同商品名稱已存在，請重新輸入商品名稱。");
        }

        mapper.updateEntityFromDto(dto, existing);
        return mapper.toDto(repository.save(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到商品 ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getActiveProducts() {
        return repository.findByActiveTrue().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getWithRelations(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到商品 ID: " + id));
        product.getSales().size();
        product.getOrderItems().size();
        return mapper.toDto(product);
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("刪除失敗，找不到商品 ID: " + id);
        }
        repository.deleteById(id);
    }
}
