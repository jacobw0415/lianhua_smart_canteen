package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.dto.expense.*;
import com.lianhua.erp.mapper.ExpenseMapper;
import com.lianhua.erp.repository.*;
import com.lianhua.erp.service.ExpenseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {
    
    private final ExpenseRepository repository;
    private final ExpenseCategoryRepository categoryRepository;
    private final EmployeeRepository employeeRepository;
    private final ExpenseMapper mapper;
    
    // ✅ 統一格式化器（會計期間）
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    
    @Override
    @Transactional
    public ExpenseDto create(ExpenseRequestDto dto) {
        // 查詢類別
        var category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID: " + dto.getCategoryId()));
        
        boolean isSalaryCategory = category.getName() != null && category.getName().contains("薪資");
        
        if (isSalaryCategory) {
            if (dto.getEmployeeId() == null) {
                throw new IllegalStateException("薪資類支出必須指定員工。");
            }
            
            var employee = employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID: " + dto.getEmployeeId()));
            
            // ✅ 強制覆蓋金額為員工薪資
            dto.setAmount(employee.getSalary());
        } else {
            if (dto.getEmployeeId() != null) {
                throw new IllegalStateException("非薪資類支出不可指定員工。");
            }
        }
        
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("支出金額必須大於 0。");
        }
        
        Expense expense = mapper.toEntity(dto);
        expense.setCategory(category);
        
        if (dto.getEmployeeId() != null) {
            expense.setEmployee(employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID: " + dto.getEmployeeId())));
        }
        
        // ✅ 新增：自動設定會計期間（依 expenseDate 為準）
        if (expense.getExpenseDate() != null) {
            expense.setAccountingPeriod(expense.getExpenseDate().format(PERIOD_FORMAT));
        } else {
            expense.setAccountingPeriod(LocalDate.now().format(PERIOD_FORMAT));
        }
        
        try {
            Expense saved = repository.save(expense);
            return mapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("資料重複：同員工、日期與類別之支出已存在。", e);
        }
    }
    
    @Override
    @Transactional
    public ExpenseDto update(Long id, ExpenseRequestDto dto) {
        Expense entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到支出 ID: " + id));
        
        //  開支日期不可修改（防止報表錯位）
        if (!entity.getExpenseDate().equals(dto.getExpenseDate())) {
            throw new IllegalStateException("開支日期不可修改，若需異動請建立新紀錄。");
        }
        
        //  類別若要修改，必須存在且不違反薪資邏輯
        if (dto.getCategoryId() != null && !dto.getCategoryId().equals(entity.getCategory().getId())) {
            ExpenseCategory newCategory = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID: " + dto.getCategoryId()));
            
            // 若類別變動涉及薪資員工，需額外防呆
            boolean wasSalary = entity.getCategory().getName().contains("薪資");
            boolean nowSalary = newCategory.getName().contains("薪資");
            if (wasSalary != nowSalary) {
                throw new IllegalStateException("不可將薪資類支出改為非薪資類（或反之）。");
            }
            
            entity.setCategory(newCategory);
        }
        
        //  員工關聯僅在薪資類別可修改
        if (dto.getEmployeeId() != null) {
            if (!entity.getCategory().getName().contains("薪資")) {
                throw new IllegalStateException("僅薪資類別可設定員工。");
            }
            entity.setEmployee(employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID: " + dto.getEmployeeId())));
        }
        
        //  若金額異動，需記錄警示（未建立 Audit Table 可先記 Log）
        if (entity.getAmount().compareTo(dto.getAmount()) != 0) {
            log.warn("💰 開支金額異動：ID={} | 原金額={} | 新金額={}",
                    id, entity.getAmount(), dto.getAmount());
        }
        
        // ✅ 可自由修改的欄位
        entity.setAmount(dto.getAmount());
        entity.setNote(dto.getNote());
        
        // ✅ accountingPeriod 不可修改，故此處不動
        
        Expense updated = repository.save(entity);
        return mapper.toDto(updated);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public ExpenseDto findById(Long id) {
        return repository.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到支出 ID: " + id));
    }
    
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("找不到支出 ID: " + id);
        }
        repository.deleteById(id);
    }
}
