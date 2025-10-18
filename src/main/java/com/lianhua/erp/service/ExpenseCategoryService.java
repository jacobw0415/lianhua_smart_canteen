package com.lianhua.erp.service;

import com.lianhua.erp.dto.expense.ExpenseCategoryDto;
import com.lianhua.erp.dto.expense.ExpenseCategoryRequestDto;

import java.util.List;

public interface ExpenseCategoryService {
    
    ExpenseCategoryDto create(ExpenseCategoryRequestDto dto);
    
    ExpenseCategoryDto update(Long id, ExpenseCategoryRequestDto dto);
    
    /**
     * 查詢所有費用類別
     * @param activeOnly 是否僅查詢啟用中項目
     * @return 費用類別清單
     */
    List<ExpenseCategoryDto> findAll(boolean activeOnly);
    
    ExpenseCategoryDto findById(Long id);
    
    void delete(Long id);
}
