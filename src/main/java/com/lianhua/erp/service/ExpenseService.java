package com.lianhua.erp.service;

import com.lianhua.erp.dto.expense.ExpenseDto;
import com.lianhua.erp.dto.expense.ExpenseRequestDto;
import com.lianhua.erp.dto.expense.ExpenseSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExpenseService {
    ExpenseDto create(ExpenseRequestDto dto);
    ExpenseDto update(Long id, ExpenseRequestDto dto);
    Page<ExpenseDto> findAll(Pageable pageable);
    ExpenseDto findById(Long id);
    
    /**
     * 作廢支出記錄
     * @param id 支出記錄 ID
     * @param reason 作廢原因
     * @return 作廢後的支出記錄 DTO
     */
    ExpenseDto voidExpense(Long id, String reason);
    
    /**
     * 搜尋費用支出（支援模糊搜尋與分頁）
     * @param req 搜尋條件
     * @param pageable 分頁參數
     * @return 分頁結果
     */
    Page<ExpenseDto> searchExpenses(ExpenseSearchRequest req, Pageable pageable);
}

