package com.lianhua.erp.service;

import com.lianhua.erp.dto.expense.ExpenseCategoryRequestDto;
import com.lianhua.erp.dto.expense.ExpenseCategoryResponseDto;
import com.lianhua.erp.dto.expense.ExpenseCategorySearchRequest;

import java.util.List;

public interface ExpenseCategoryService {
    
    ExpenseCategoryResponseDto create(ExpenseCategoryRequestDto dto);
    
    ExpenseCategoryResponseDto update(Long id, ExpenseCategoryRequestDto dto);
    
    /**
     * 查詢所有費用類別
     * @return 費用類別清單
     */
    List<ExpenseCategoryResponseDto> getAll();
    
    /**
     * 查詢啟用中的費用類別
     * @return 啟用中的費用類別清單
     */
    List<ExpenseCategoryResponseDto> getActive();
    
    ExpenseCategoryResponseDto getById(Long id);
    
    ExpenseCategoryResponseDto activate(Long id);
    
    ExpenseCategoryResponseDto deactivate(Long id);
    
    List<ExpenseCategoryResponseDto> search(ExpenseCategorySearchRequest search);
    
    void delete(Long id);
}
