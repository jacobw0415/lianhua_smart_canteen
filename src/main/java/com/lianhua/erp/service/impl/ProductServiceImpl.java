package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Product;
import com.lianhua.erp.domain.ProductCategory;
import com.lianhua.erp.dto.product.*;
import com.lianhua.erp.mapper.ProductMapper;
import com.lianhua.erp.repository.ProductRepository;
import com.lianhua.erp.repository.ProductCategoryRepository;
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
    private final ProductCategoryRepository categoryRepository; // 🔹 新增：分類存取用
    private final ProductMapper mapper;

    @Override
    public ProductResponseDto create(ProductRequestDto dto) {
        // ✅ 檢查商品名稱唯一性
        if (repository.existsByName(dto.getName())) {
            throw new DataIntegrityViolationException("相同商品名稱已存在，請重新輸入商品名稱。");
        }

        // ✅ 檢查分類是否存在
        ProductCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("找不到分類 ID: " + dto.getCategoryId()));

        // ✅ 轉換 DTO → Entity 並設定分類
        Product product = mapper.toEntity(dto);
        product.setCategory(category);

        // ✅ 儲存並轉換回 DTO
        return mapper.toDto(repository.save(product));
    }

    @Override
    public ProductResponseDto update(Long id, ProductRequestDto dto) {
        Product existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到商品 ID: " + id));

        // 若名稱改變，需檢查是否重複
        if (!existing.getName().equalsIgnoreCase(dto.getName()) && repository.existsByName(dto.getName())) {
            throw new DataIntegrityViolationException("相同商品名稱已存在，請重新輸入商品名稱。");
        }

        // ✅ 檢查是否更新分類
        if (dto.getCategoryId() != null) {
            ProductCategory category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到分類 ID: " + dto.getCategoryId()));
            existing.setCategory(category);
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

        // 預先載入關聯資料
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

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("找不到分類 ID: " + categoryId);
        }
        return repository.findByCategoryId(categoryId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

}
