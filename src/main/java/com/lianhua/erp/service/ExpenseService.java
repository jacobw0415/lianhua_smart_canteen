package com.lianhua.erp.service;

import com.lianhua.erp.dto.expense.ExpenseDto;
import com.lianhua.erp.dto.expense.ExpenseRequestDto;

import java.util.List;

public interface ExpenseService {
    ExpenseDto create(ExpenseRequestDto dto);
    ExpenseDto update(Long id, ExpenseRequestDto dto);
    List<ExpenseDto> findAll();
    ExpenseDto findById(Long id);
    void delete(Long id);
}

