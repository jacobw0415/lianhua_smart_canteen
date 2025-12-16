package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.ProductCategory;
import com.lianhua.erp.dto.product.ProductCategoryRequestDto;
import com.lianhua.erp.dto.product.ProductCategoryResponseDto;
import com.lianhua.erp.dto.product.ProductCategorySearchRequest;
import com.lianhua.erp.mapper.ProductCategoryMapper;
import com.lianhua.erp.repository.ProductCategoryRepository;
import com.lianhua.erp.service.ProductCategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCategoryServiceImpl implements ProductCategoryService {
    
    private final ProductCategoryRepository repository;
    private final ProductCategoryMapper mapper;
    
    /**
     * 建立商品分類
     *
     * 錯誤處理原則：
     * - 業務規則錯誤 → ResponseStatusException
     * - DB constraint → catch 後轉業務錯誤
     */
    @Override
    public ProductCategoryResponseDto create(ProductCategoryRequestDto dto) {
        
        // === 基本必填欄位檢查 ===
        if (!StringUtils.hasText(dto.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "分類名稱為必填欄位"
            );
        }
        
        if (!StringUtils.hasText(dto.getCode())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "分類代碼為必填欄位"
            );
        }
        
        String name = dto.getName().trim();
        String code = dto.getCode().trim();
        
        // === 分類名稱唯一性（業務規則） ===
        if (repository.existsByName(name)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "分類名稱已存在，請使用其他名稱。"
            );
        }
        
        // === 分類代碼唯一性（業務規則） ===
        if (repository.existsByCode(code)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "分類代碼已存在，請使用其他代碼。"
            );
        }
        
        ProductCategory category = mapper.toEntity(dto);
        category.setName(name);
        category.setCode(code);
        
        try {
            return mapper.toDto(repository.save(category));
        } catch (DataIntegrityViolationException ex) {
            // DB 層保底防呆
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "資料重複或違反資料完整性限制，請確認分類名稱與代碼"
            );
        }
    }
    
    /**
     * 更新商品分類
     */
    @Override
    public ProductCategoryResponseDto update(Long id, ProductCategoryRequestDto dto) {
        
        ProductCategory existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "找不到分類 ID：" + id
                ));
        
        // === 分類名稱變更檢查 ===
        if (StringUtils.hasText(dto.getName())) {
            String newName = dto.getName().trim();
            
            if (!newName.equalsIgnoreCase(existing.getName())
                    && repository.existsByName(newName)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "分類名稱已存在，請使用其他名稱。"
                );
            }
            existing.setName(newName);
        }
        
        // === 分類代碼變更檢查 ===
        if (StringUtils.hasText(dto.getCode())) {
            String newCode = dto.getCode().trim();
            
            if (!newCode.equalsIgnoreCase(existing.getCode())
                    && repository.existsByCode(newCode)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "分類代碼已存在，請使用其他代碼。"
                );
            }
            existing.setCode(newCode);
        }
        
        mapper.updateEntityFromDto(dto, existing);
        
        try {
            return mapper.toDto(repository.save(existing));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "資料重複或違反資料完整性限制，請確認分類資料"
            );
        }
    }
    
    /**
     * 取得單一分類
     */
    @Override
    @Transactional(readOnly = true)
    public ProductCategoryResponseDto getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到分類 ID：" + id)
                );
    }
    
    /**
     * 取得全部分類
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponseDto> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 取得啟用中的分類
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponseDto> getActive() {
        return repository.findAll()
                .stream()
                .filter(ProductCategory::getActive)
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 停用分類
     */
    @Override
    public ProductCategoryResponseDto deactivate(Long id) {
        
        ProductCategory category = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "找不到分類 ID：" + id
                ));
        
        category.setActive(false);
        return mapper.toDto(repository.save(category));
    }
    
    /**
     * 啟用分類
     */
    @Override
    public ProductCategoryResponseDto activate(Long id) {
        
        ProductCategory category = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "找不到分類 ID：" + id
                ));
        
        category.setActive(true);
        return mapper.toDto(repository.save(category));
    }
    
    /**
     * 分類搜尋（Example + 模糊比對）
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponseDto> search(ProductCategorySearchRequest search) {
        
        ProductCategory probe = new ProductCategory();
        
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
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
        
        return repository.findAll(Example.of(probe, matcher))
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 刪除分類
     */
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("刪除失敗，找不到分類 ID：" + id);
        }
        repository.deleteById(id);
    }
}
