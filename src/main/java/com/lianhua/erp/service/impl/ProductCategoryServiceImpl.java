package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.ProductCategory;
import com.lianhua.erp.dto.product.ProductCategoryRequestDto;
import com.lianhua.erp.dto.product.ProductCategoryResponseDto;
import com.lianhua.erp.dto.product.ProductCategorySearchRequest;
import com.lianhua.erp.mapper.ProductCategoryMapper;
import com.lianhua.erp.repository.ProductCategoryRepository;
import com.lianhua.erp.repository.ProductRepository;
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
    private final ProductRepository productRepository;
    private final ProductCategoryMapper mapper;

    /**
     * 建立商品分類
     * <p>
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
                .orElseThrow(() -> new EntityNotFoundException("找不到分類 ID：" + id));

        // === 1. 分類名稱與代碼變更檢查（業務邏輯保留手動，因需排除自身與提示訊息） ===
        if (StringUtils.hasText(dto.getName())) {
            String newName = dto.getName().trim();
            if (!newName.equalsIgnoreCase(existing.getName()) && repository.existsByName(newName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類名稱已存在。");
            }
            existing.setName(newName);
        }

        if (StringUtils.hasText(dto.getCode())) {
            String newCode = dto.getCode().trim();
            if (!newCode.equalsIgnoreCase(existing.getCode()) && repository.existsByCode(newCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類代碼已存在。");
            }
            existing.setCode(newCode);
        }

        // === 2. 先呼叫 Mapper 進行自動映射（處理 active 等其他欄位） ===
        // 這裡會處理除了備註以外那些不需要特殊邏輯的欄位
        mapper.updateEntityFromDto(dto, existing);

        // === 3. 【關鍵修正】手動覆蓋描述/備註欄位，解決 IGNORE 策略無法清空的問題 ===
        if (dto.getDescription() != null) {
            String description = dto.getDescription().trim();
            if (description.length() > 255) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類描述長度不可超過255個字元。");
            }
            existing.setDescription(description);
        } else {
            // 發生在 Mapper 之後，強制將描述設為 null，達到清空效果
            existing.setDescription(null);
        }

        try {
            return mapper.toDto(repository.save(existing));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資料重複或違反限制");
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

        // 分類不存在
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "刪除失敗，找不到商品分類 ID：" + id
            );
        }

        // 分類已被商品使用 → 不允許刪除
        if (productRepository.existsByCategoryId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "此商品類別已有創建商品，無法刪除，請改為停用!"
            );
        }

        // 可安全刪除
        repository.deleteById(id);
    }
}
