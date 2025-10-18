package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.*;
import com.lianhua.erp.dto.expense.*;
import com.lianhua.erp.mapper.ExpenseMapper;
import com.lianhua.erp.repository.*;
import com.lianhua.erp.service.ExpenseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseServiceImpl implements ExpenseService {
    
    private final ExpenseRepository repository;
    private final ExpenseCategoryRepository categoryRepository;
    private final EmployeeRepository employeeRepository;
    private final ExpenseMapper mapper;
    
    @Override
    public ExpenseDto create(ExpenseRequestDto dto) {
        Expense expense = mapper.toEntity(dto);
        expense.setCategory(categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID: " + dto.getCategoryId())));
        
        if (dto.getEmployeeId() != null) {
            expense.setEmployee(employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID: " + dto.getEmployeeId())));
        }
        
        try {
            return mapper.toDto(repository.save(expense));
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("資料重複：同員工、日期與類別之支出已存在。", e);
        }
    }
    
    @Override
    public ExpenseDto update(Long id, ExpenseRequestDto dto) {
        Expense entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到支出 ID: " + id));
        
        entity.setExpenseDate(dto.getExpenseDate());
        entity.setAmount(dto.getAmount());
        entity.setNote(dto.getNote());
        
        if (dto.getCategoryId() != null)
            entity.setCategory(categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID: " + dto.getCategoryId())));
        
        if (dto.getEmployeeId() != null)
            entity.setEmployee(employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID: " + dto.getEmployeeId())));
        
        return mapper.toDto(repository.save(entity));
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
