package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.expense.ExpenseDto;
import com.lianhua.erp.domin.Expense;
import com.lianhua.erp.mapper.ExpenseMapper;
import com.lianhua.erp.repository.ExpenseRepository;
import com.lianhua.erp.service.ExpenseService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper expenseMapper;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository, ExpenseMapper expenseMapper) {
        this.expenseRepository = expenseRepository;
        this.expenseMapper = expenseMapper;
    }

    @Override
    public List<ExpenseDto> getAllExpenses() {
        return expenseRepository.findAll()
                .stream()
                .map(expenseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ExpenseDto getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + id));
        return expenseMapper.toDto(expense);
    }

    @Override
    public ExpenseDto createExpense(ExpenseDto dto) {
        Expense expense = expenseMapper.toEntity(dto);
        return expenseMapper.toDto(expenseRepository.save(expense));
    }

    @Override
    public ExpenseDto updateExpense(Long id, ExpenseDto dto) {
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + id));

        Expense updated = expenseMapper.toEntity(dto);
        updated.setId(existing.getId()); // 保持 ID 不變

        return expenseMapper.toDto(expenseRepository.save(updated));
    }

    @Override
    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }
}
