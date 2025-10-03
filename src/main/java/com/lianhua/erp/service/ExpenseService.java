package com.lianhua.erp.service;

import com.lianhua.erp.dto.ExpenseDto;
import java.util.List;

public interface ExpenseService {
    List<ExpenseDto> getAllExpenses();
    ExpenseDto getExpenseById(Long id);
    ExpenseDto createExpense(ExpenseDto dto);
    ExpenseDto updateExpense(Long id, ExpenseDto dto);
    void deleteExpense(Long id);
}

