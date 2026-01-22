package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.ExpenseCategory;
import com.lianhua.erp.dto.expense.ExpenseCategoryRequestDto;
import com.lianhua.erp.dto.expense.ExpenseCategoryResponseDto;
import com.lianhua.erp.dto.expense.ExpenseCategorySearchRequest;
import com.lianhua.erp.mapper.ExpenseCategoryMapper;
import com.lianhua.erp.repository.ExpenseCategoryRepository;
import com.lianhua.erp.repository.ExpenseRepository;
import com.lianhua.erp.service.ExpenseCategoryService;
import com.lianhua.erp.service.impl.spec.ExpenseCategorySpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
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
@Slf4j
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {

    private final ExpenseCategoryRepository repository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryMapper mapper;


    // ============================================
    // 查詢全部
    // ============================================
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "expenseCategories", key = "'all'")
    public List<ExpenseCategoryResponseDto> getAll() {
        return repository.findAllByOrderByAccountCodeAsc()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // ============================================
    // 查詢啟用中的
    // ============================================
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "expenseCategories", key = "'active'")
    public List<ExpenseCategoryResponseDto> getActive() {
        return repository.findAllByActiveTrueOrderByAccountCodeAsc()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // ============================================
    // 查詢單筆
    // ============================================
    @Override
    @Transactional(readOnly = true)
    public ExpenseCategoryResponseDto getById(Long id) {
        ExpenseCategory category = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID：" + id));
        return mapper.toDto(category);
    }

    // ============================================
    // 新增類別（平鋪式設計）
    // ============================================
    @Override
    @CacheEvict(value = "expenseCategories", allEntries = true)
    public ExpenseCategoryResponseDto create(ExpenseCategoryRequestDto dto) {

        // === 基本必填欄位檢查 ===
        if (!StringUtils.hasText(dto.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "費用類別名稱為必填欄位"
            );
        }

        String name = dto.getName().trim();

        // === 名稱長度檢查 ===
        if (name.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "費用類別名稱長度不可超過100個字元。"
            );
        }

        // === 名稱唯一性檢查（忽略大小寫）===
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "費用類別名稱已存在，請使用其他名稱。"
            );
        }

        // === 相似名稱檢查（防止重複類似類別）===
        String similarName = checkSimilarName(name, null);
        if (similarName != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("費用類別名稱「%s」與現有類別「%s」過於相似，請使用其他名稱。", name, similarName)
            );
        }

        ExpenseCategory category = new ExpenseCategory();
        mapper.updateEntityFromDto(dto, category);
        category.setName(name);

        // === 自動生成會計代碼（EXP-001, EXP-002, EXP-003...）===
        String nextCode = generateNextAccountCode();
        if (nextCode == null || nextCode.isBlank()) {
            log.error("無法生成 accountCode");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "系統錯誤：無法生成 accountCode"
            );
        }
        category.setAccountCode(nextCode);

        // === 儲存（攔截 DB constraint）===
        try {
            ExpenseCategory saved = repository.save(category);
            log.info("成功創建費用類別: {} (ID: {}, Code: {})", saved.getName(), saved.getId(), saved.getAccountCode());
            return mapper.toDto(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("創建費用類別失敗，違反資料完整性限制: {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "資料重複或違反資料完整性限制，請確認費用類別名稱或代碼"
            );
        }
    }

    // ============================================
    // 更新類別
    // ============================================
    @Override
    @Transactional
    @CacheEvict(value = "expenseCategories", allEntries = true)
    public ExpenseCategoryResponseDto update(Long id, ExpenseCategoryRequestDto dto) {
        ExpenseCategory existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID：" + id));

        // === 名稱變更檢查 ===
        if (StringUtils.hasText(dto.getName())) {
            String newName = dto.getName().trim();

            // === 名稱長度檢查 ===
            if (newName.length() > 100) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "費用類別名稱長度不可超過100個字元。"
                );
            }

            // === 名稱唯一性檢查（排除自身）===
            if (!newName.equalsIgnoreCase(existing.getName())
                    && repository.existsByNameIgnoreCaseAndIdNot(newName, id)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "費用類別名稱已存在，請使用其他名稱。"
                );
            }

            // === 相似名稱檢查（防止重複類似類別，排除自身）===
            String similarName = checkSimilarName(newName, id);
            if (similarName != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("費用類別名稱「%s」與現有類別「%s」過於相似，請使用其他名稱。", newName, similarName)
                );
            }

            existing.setName(newName);
        }

        // === 更新允許欄位（描述、啟用狀態、是否為薪資類別、頻率類型）===
        if (dto.getDescription() != null) {
            String description = dto.getDescription().trim();
            if (description.length() > 255) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "費用說明長度不可超過255個字元。");
            }
            existing.setDescription(description);
        } else {
            // 如果前端傳 null，代表要清空該欄位
            existing.setDescription(null);
        }
        if (dto.getActive() != null) {
            existing.setActive(dto.getActive());
        }
        if (dto.getIsSalary() != null) {
            // === 檢查是否可修改 isSalary（如果類別已被使用，不允許修改）===
            boolean wasSalary = Boolean.TRUE.equals(existing.getIsSalary());
            boolean nowSalary = Boolean.TRUE.equals(dto.getIsSalary());
            if (wasSalary != nowSalary) {
                // 檢查類別是否已被支出記錄使用
                if (expenseRepository.existsByCategoryId(id)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "此費用類別已有創建費用記錄，無法修改「是否為薪資類別」屬性。如需修改，請先處理相關的費用記錄。");
                }
            }
            existing.setIsSalary(dto.getIsSalary());
        }
        if (dto.getFrequencyType() != null) {
            existing.setFrequencyType(dto.getFrequencyType());
        }

        // === 儲存（攔截 DB constraint）===
        try {
            ExpenseCategory updated = repository.save(existing);
            log.info("成功更新費用類別: {} (ID: {}, Code: {})", updated.getName(), updated.getId(), updated.getAccountCode());
            return mapper.toDto(updated);
        } catch (DataIntegrityViolationException ex) {
            log.warn("更新費用類別失敗，違反資料完整性限制: {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "資料重複或違反資料完整性限制，請確認費用類別資料"
            );
        }
    }

    // ============================================
    // 啟用類別
    // ============================================
    @Override
    @CacheEvict(value = "expenseCategories", allEntries = true)
    public ExpenseCategoryResponseDto activate(Long id) {
        ExpenseCategory category = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID：" + id));

        category.setActive(true);
        ExpenseCategory saved = repository.save(category);
        log.info("成功啟用費用類別: {} (ID: {})", saved.getName(), saved.getId());
        return mapper.toDto(saved);
    }

    // ============================================
    // 停用類別
    // ============================================
    @Override
    @CacheEvict(value = "expenseCategories", allEntries = true)
    public ExpenseCategoryResponseDto deactivate(Long id) {
        ExpenseCategory category = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID：" + id));

        category.setActive(false);
        ExpenseCategory saved = repository.save(category);
        log.info("成功停用費用類別: {} (ID: {})", saved.getName(), saved.getId());
        return mapper.toDto(saved);
    }

    // ============================================
    // 搜尋類別（使用 Specification 實現模糊搜尋）
    // ============================================
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponseDto> search(ExpenseCategorySearchRequest search) {

        Specification<ExpenseCategory> spec = ExpenseCategorySpecifications.build(search);

        List<ExpenseCategory> results = repository.findAll(spec);

        return results.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // ============================================
    // 刪除類別
    // ============================================
    @Override
    @CacheEvict(value = "expenseCategories", allEntries = true)
    public void delete(Long id) {

        ExpenseCategory category = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID：" + id));

        // === 檢查是否被費用記錄使用 ===
        if (expenseRepository.existsByCategoryId(id)) {
            log.warn("嘗試刪除已被費用記錄使用的費用類別: {} (ID: {})", category.getName(), id);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "此費用類別已有創建費用記錄，無法刪除，請改為停用!"
            );
        }

        repository.delete(category);
        log.info("成功刪除費用類別: {} (ID: {})", category.getName(), id);
    }

    // ============================================
    // 自動產生會計科目代碼邏輯 (EXP-001, EXP-002, EXP-003...)
    // ============================================
    private String generateNextAccountCode() {
        // 查找所有類別，按代碼降序排列
        List<ExpenseCategory> allCategories = repository.findAllOrderByAccountCodeDesc();

        // 如果沒有類別，從 EXP-001 開始
        if (allCategories.isEmpty()) {
            return "EXP-001";
        }

        // 找到最大的編號
        int maxNum = allCategories.stream()
                .map(cat -> parseCodeNumber(cat.getAccountCode()))
                .max(Integer::compareTo)
                .orElse(0);

        return String.format("EXP-%03d", maxNum + 1);
    }

    /**
     * 解析代碼中的數字部分（例如：EXP-001 -> 1, EXP-123 -> 123）
     */
    private int parseCodeNumber(String code) {
        try {
            String numStr = code.replace("EXP-", "");
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            log.warn("無法解析會計代碼: {}", code);
            return 0;
        }
    }

    /**
     * 檢查名稱是否與現有類別過於相似（防止重複類似類別）
     * <p>
     * 相似性判斷規則：
     * 1. 如果新名稱是現有名稱的子串（或相反），則視為相似
     * 2. 例如：「房租」與「房租费」、「房租费」與「房租」視為相似
     *
     * @param newName   新名稱（已 trim）
     * @param excludeId 要排除的 ID（用於更新時排除自身，創建時傳 null）
     * @return 如果找到相似的名稱，返回該名稱；否則返回 null
     */
    private String checkSimilarName(String newName, Long excludeId) {
        String newNameLower = newName.toLowerCase();
        List<String> existingNames;

        if (excludeId != null) {
            existingNames = repository.findAllNamesExcludingId(excludeId);
        } else {
            existingNames = repository.findAllNames();
        }

        for (String existingName : existingNames) {
            String existingNameLower = existingName.toLowerCase();

            // 如果新名稱是現有名稱的子串（例如：「房租」是「房租费」的子串）
            if (existingNameLower.contains(newNameLower) && !existingNameLower.equals(newNameLower)) {
                log.warn("檢測到相似名稱：新名稱「{}」是現有名稱「{}」的子串", newName, existingName);
                return existingName;
            }

            // 如果現有名稱是新名稱的子串（例如：「房租费」包含「房租」）
            if (newNameLower.contains(existingNameLower) && !newNameLower.equals(existingNameLower)) {
                log.warn("檢測到相似名稱：現有名稱「{}」是新名稱「{}」的子串", existingName, newName);
                return existingName;
            }
        }

        return null;
    }
}
