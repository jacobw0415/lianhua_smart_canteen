package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Product;
import com.lianhua.erp.domain.ProductCategory;
import com.lianhua.erp.dto.product.ProductRequestDto;
import com.lianhua.erp.dto.product.ProductResponseDto;
import com.lianhua.erp.dto.product.ProductSearchRequest;
import com.lianhua.erp.mapper.ProductMapper;
import com.lianhua.erp.repository.ProductCategoryRepository;
import com.lianhua.erp.repository.ProductRepository;
import com.lianhua.erp.repository.SalesRepository;
import com.lianhua.erp.service.ProductService;
import com.lianhua.erp.service.impl.spec.ProductSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository repository;
    private final SalesRepository salesRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductMapper mapper;
    
    /**
     * 建立商品
     *
     * 錯誤處理原則（對齊 PurchaseServiceImpl）：
     * - 業務規則錯誤 → ResponseStatusException
     * - DB constraint 錯誤 → catch 後轉 ResponseStatusException
     */
    @Override
    public ProductResponseDto create(ProductRequestDto dto) {
        
        // === 基本必填欄位檢查 ===
        if (!StringUtils.hasText(dto.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "商品名稱為必填欄位"
            );
        }
        
        if (dto.getCategoryId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "商品分類為必填欄位"
            );
        }
        
        // === 商品名稱唯一性（業務規則） ===
        String name = dto.getName().trim();
        if (repository.existsByName(name)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "相同商品名稱已存在，請重新輸入商品名稱。"
            );
        }
        
        // === 分類存在性檢查 ===
        ProductCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "找不到商品分類 ID：" + dto.getCategoryId()
                ));
        
        // === 建立 Entity 並設定關聯 ===
        Product product = mapper.toEntity(dto);
        product.setName(name);
        product.setCategory(category);
        
        // === 儲存（攔截 DB constraint） ===
        try {
            return mapper.toDto(repository.save(product));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "資料重複或違反資料完整性限制，請確認商品名稱是否已存在"
            );
        }
    }
    
    /**
     * 更新商品
     *
     * - 僅在名稱實際變更時檢查唯一性
     * - 分類變更時檢查分類是否存在
     */
    @Override
    public ProductResponseDto update(Long id, ProductRequestDto dto) {
        
        Product existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "找不到商品 ID：" + id
                ));
        
        // === 商品名稱變更檢查 ===
        if (StringUtils.hasText(dto.getName())) {
            String newName = dto.getName().trim();
            String existingName = existing.getName();
            
            // 只有在名稱實際變更時才檢查唯一性（使用精確比較，因為資料庫 UNIQUE 約束是區分大小寫的）
            if (!newName.equals(existingName)) {
                // 檢查是否有其他商品使用相同名稱（排除當前商品）
                if (repository.existsByNameAndIdNot(newName, id)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "相同商品名稱已存在，請重新輸入商品名稱。"
                    );
                }
                existing.setName(newName);
            }
        }
        
        // === 分類更新檢查 ===
        if (dto.getCategoryId() != null) {
            ProductCategory category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "找不到商品分類 ID：" + dto.getCategoryId()
                    ));
            existing.setCategory(category);
        }
        
        // === 套用其餘可更新欄位（name 和 category 已在 Mapper 中排除，由上面手動處理） ===
        mapper.updateEntityFromDto(dto, existing);
        
        try {
            return mapper.toDto(repository.save(existing));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "資料重複或違反資料完整性限制，請確認商品名稱是否已存在"
            );
        }
    }
    
    /**
     * 取得單一商品
     */
    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到商品 ID：" + id)
                );
    }
    
    /**
     * 停用商品
     */
    @Override
    public ProductResponseDto deactivate(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "找不到商品 ID：" + id
                ));
        
        product.setActive(false);
        return mapper.toDto(repository.save(product));
    }
    
    /**
     * 啟用商品
     */
    @Override
    public ProductResponseDto activate(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "找不到商品 ID：" + id
                ));
        
        product.setActive(true);
        return mapper.toDto(repository.save(product));
    }
    
    /**
     * 取得全部商品（不含關聯）
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 取得啟用中的商品
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getActiveProducts() {
        return repository.findByActiveTrue()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 取得商品（含關聯資料）
     */
    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getWithRelations(Long id) {
        
        Product product = repository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("找不到商品 ID：" + id)
                );
        
        // 預先載入關聯，避免 LazyInitializationException
        product.getSales().size();
        product.getOrderItems().size();
        
        return mapper.toDto(product);
    }
    
    /**
     * 商品搜尋（Specification）
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> search(ProductSearchRequest search) {
        
        return repository.findAll(ProductSpecification.build(search))
                .stream()
                .map(mapper::toDto)
                .toList();
    }
    
    /**
     * 刪除商品
     */
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("刪除失敗，找不到商品 ID：" + id);
        }

        if (salesRepository.existsByProductId(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "此商品已有銷售紀錄，無法刪除，請改為停用!");
        }
        repository.deleteById(id);
    }
    
    /**
     * 依分類取得商品
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getByCategory(Long categoryId) {
        
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("找不到分類 ID：" + categoryId);
        }
        
        return repository.findByCategoryId(categoryId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
