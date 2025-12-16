package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.ProductCategory;
import com.lianhua.erp.dto.product.*;
import com.lianhua.erp.mapper.ProductCategoryMapper;
import com.lianhua.erp.repository.ProductCategoryRepository;
import com.lianhua.erp.service.ProductCategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository repository;
    private final ProductCategoryMapper mapper;

    @Override
    public ProductCategoryResponseDto create(ProductCategoryRequestDto dto) {
        if (repository.existsByName(dto.getName())) {
            throw new DataIntegrityViolationException("分類名稱已存在，請使用其他名稱。");
        }
        if (repository.existsByCode(dto.getCode())) {
            throw new DataIntegrityViolationException("分類代碼已存在，請使用其他代碼。");
        }
        ProductCategory category = mapper.toEntity(dto);
        return mapper.toDto(repository.save(category));
    }

    @Override
    public ProductCategoryResponseDto update(Long id, ProductCategoryRequestDto dto) {
        ProductCategory existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到分類 ID: " + id));

        if (!existing.getName().equalsIgnoreCase(dto.getName()) && repository.existsByName(dto.getName())) {
            throw new DataIntegrityViolationException("分類名稱已存在。");
        }
        if (!existing.getCode().equalsIgnoreCase(dto.getCode()) && repository.existsByCode(dto.getCode())) {
            throw new DataIntegrityViolationException("分類代碼已存在。");
        }

        mapper.updateEntityFromDto(dto, existing);
        return mapper.toDto(repository.save(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCategoryResponseDto getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到分類 ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponseDto> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponseDto> getActive() {
        return repository.findAll()
                .stream()
                .filter(ProductCategory::getActive)
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductCategoryResponseDto deactivate(Long id) {
        ProductCategory category = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到分類 ID: " + id));
        category.setActive(false);
        return mapper.toDto(repository.save(category));
    }

    @Override
    public ProductCategoryResponseDto activate(Long id) {
        ProductCategory category = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到分類 ID: " + id));
        category.setActive(true);
        return mapper.toDto(repository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponseDto> search(ProductCategorySearchRequest search) {

        ProductCategory probe = new ProductCategory();

        // ===== 模糊搜尋條件（與 Supplier / Purchase 對齊） =====

        if (StringUtils.hasText(search.getName())) {
            probe.setName(search.getName().trim());
        }

        if (StringUtils.hasText(search.getCode())) {
            probe.setCode(search.getCode().trim());
        }

        if (search.getActive() != null) {
            probe.setActive(search.getActive());
        }

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING); // ⭐ 模糊搜尋

        Example<ProductCategory> example = Example.of(probe, matcher);

        return repository.findAll(example)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("刪除失敗，找不到分類 ID: " + id);
        }
        repository.deleteById(id);
    }
}
