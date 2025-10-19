package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.ExpenseCategory;
import com.lianhua.erp.dto.expense.ExpenseCategoryDto;
import com.lianhua.erp.dto.expense.ExpenseCategoryRequestDto;
import com.lianhua.erp.mapper.ExpenseCategoryMapper;
import com.lianhua.erp.repository.ExpenseCategoryRepository;
import com.lianhua.erp.service.ExpenseCategoryService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {
    
    private final ExpenseCategoryRepository repository;
    private final ExpenseCategoryMapper mapper;
    
    
    // ============================================
    // 查詢全部
    // ============================================
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseCategoryDto> findAll(boolean activeOnly) {
        List<ExpenseCategory> categories = activeOnly
                ? repository.findAllByActiveTrueOrderByAccountCodeAsc()
                : repository.findAllByOrderByAccountCodeAsc();
        
        return categories.stream()
                .map(mapper::toDto)
                .toList();
    }
    
    // ============================================
    // 查詢單筆
    // ============================================
    @Override
    @Transactional(readOnly = true)
    public ExpenseCategoryDto findById(Long id) {
        ExpenseCategory category = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID：" + id));
        return mapper.toDto(category);
    }
    
    // ============================================
    // 新增類別（自動建立 ROOT + 自動帶入 parentId）
    // ============================================
    @Override
    public ExpenseCategoryDto create(ExpenseCategoryRequestDto dto) {
        // 1️⃣ 名稱唯一性（忽略大小寫）
        if (repository.existsByNameIgnoreCase(dto.getName())) {
            throw new DataIntegrityViolationException("費用類別名稱已存在：" + dto.getName());
        }
        
        ExpenseCategory category = new ExpenseCategory();
        mapper.updateEntityFromDto(dto, category);
        
        ExpenseCategory rootCategory = null;
        
        // 2️⃣ 若表為空 → 自動建立 ROOT 節點
        if (repository.count() == 0) {
            rootCategory = ExpenseCategory.builder()
                    .name("費用類別總帳")
                    .accountCode("EXP-000")
                    .description("系統自動建立的費用分類根節點，用作所有費用類別的上層總帳")
                    .active(true)
                    .parent(null)
                    .build();
            rootCategory = repository.save(rootCategory);
            log.info("✅ 系統自動建立 ROOT 節點：{}", rootCategory.getName());
        }
        
        // 3️⃣ 若 parentId 為空 → 自動設為 ROOT
        if (dto.getParentId() == null) {
            ExpenseCategory root = rootCategory != null
                    ? rootCategory
                    : repository.findByNameIgnoreCase("費用類別總帳")
                    .orElseThrow(() -> new EntityNotFoundException("找不到根節點 '費用類別總帳'"));
            category.setParent(root);
        } else {
            // 若指定 parentId，需驗證存在
            ExpenseCategory parent = repository.findById(dto.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到上層費用類別 ID：" + dto.getParentId()));
            category.setParent(parent);
        }
        
        // 4️⃣ 自動生成會計代碼（⚠️ 需防呆，防止回傳 null）
        String nextCode = generateNextAccountCode(category.getParent());
        if (nextCode.isBlank()) {
            throw new IllegalStateException("系統錯誤：無法生成 accountCode");
        }
        category.setAccountCode(nextCode);
        
        // 5️⃣ 儲存
        ExpenseCategory saved = repository.save(category);
        return mapper.toDto(saved);
    }
    
    // ============================================
    // 更新類別
    // ============================================
    @Override
    @Transactional
    public ExpenseCategoryDto update(Long id, ExpenseCategoryRequestDto dto) {
        ExpenseCategory existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID：" + id));
        
        // 1️⃣ 根節點（費用類別總帳）不可修改
        if ("EXP-000".equals(existing.getAccountCode())) {
            throw new IllegalStateException("根節點「費用類別總帳」不可修改。");
        }
        
        //  2️⃣ 禁止修改 accountCode（系統生成）
        if (dto.getAccountCode() != null && !dto.getAccountCode().isBlank()) {
            throw new IllegalStateException("會計科目代碼不可修改，該欄位由系統自動生成。");
        }
        
        //  3️⃣ 名稱重複檢查（忽略大小寫）
        if (!existing.getName().equalsIgnoreCase(dto.getName())
                && repository.existsByNameIgnoreCase(dto.getName())) {
            throw new DataIntegrityViolationException("費用類別名稱已存在：" + dto.getName());
        }
        
        //  4️⃣ 若修改 parentId → 防止循環或錯誤層級
        if (dto.getParentId() != null && (existing.getParent() == null
                || !existing.getParent().getId().equals(dto.getParentId()))) {
            
            ExpenseCategory newParent = repository.findById(dto.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到上層費用類別 ID：" + dto.getParentId()));
            
            // 防止自己成為自己的上層
            if (newParent.getId().equals(existing.getId())) {
                throw new IllegalStateException("費用類別不可指定自己為上層。");
            }
            
            existing.setParent(newParent);
        }
        
        // ⚙️ 5️⃣ 更新允許欄位（描述、啟用狀態、名稱）
        existing.setDescription(dto.getDescription());
        existing.setActive(dto.getActive() != null ? dto.getActive() : existing.getActive());
        existing.setName(dto.getName());
        
        // ✅ 6️⃣ 若 accountCode 為空（防呆補碼）
        if (existing.getAccountCode() == null || existing.getAccountCode().isBlank()) {
            existing.setAccountCode(generateNextAccountCode(existing.getParent()));
        }
        
        ExpenseCategory updated = repository.save(existing);
        return mapper.toDto(updated);
    }
    
    // ============================================
    // 刪除類別（防止刪除 ROOT）
    // ============================================
    @Override
    public void delete(Long id) {
        ExpenseCategory category = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID：" + id));
        
        if ("EXP-000".equalsIgnoreCase(category.getAccountCode())) {
            throw new DataIntegrityViolationException("無法刪除根節點 '費用類別總帳'");
        }
        
        repository.delete(category);
    }
    
    // ============================================
    // 自動產生會計科目代碼邏輯 (EXP-001 ~)
    // ============================================
    private String generateNextAccountCode(ExpenseCategory parent) {
        if (parent == null) {
            Optional<ExpenseCategory> last = repository.findTopLevelLastCode();
            String lastCode = last.map(ExpenseCategory::getAccountCode).orElse("EXP-000");
            int nextNum = parseLastNumber(lastCode) + 1;
            return String.format("EXP-%03d", nextNum);
        }
        
        String prefix = parent.getAccountCode();
        Optional<ExpenseCategory> lastChild = repository.findTopByParentOrderByAccountCodeDesc(parent);
        int nextSubNum = lastChild.map(c -> parseSubCode(c.getAccountCode(), prefix)).orElse(0) + 1;
        
        return String.format("%s-%02d", prefix, nextSubNum);
    }
    
    private int parseLastNumber(String code) {
        try { return Integer.parseInt(code.replace("EXP-", "")); }
        catch (NumberFormatException e) { return 0; }
    }
    
    private int parseSubCode(String childCode, String prefix) {
        try { return Integer.parseInt(childCode.replace(prefix + "-", "")); }
        catch (NumberFormatException e) { return 0; }
    }
}
