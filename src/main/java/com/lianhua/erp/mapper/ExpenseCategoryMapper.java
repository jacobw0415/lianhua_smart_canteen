package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.ExpenseCategory;
import com.lianhua.erp.dto.expense.ExpenseCategoryRequestDto;
import com.lianhua.erp.dto.expense.ExpenseCategoryResponseDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ExpenseCategoryMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountCode", ignore = true)
    ExpenseCategory toEntity(ExpenseCategoryRequestDto dto);
    
    ExpenseCategoryResponseDto toDto(ExpenseCategory entity);
    
    @Mapping(target = "accountCode", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(ExpenseCategoryRequestDto dto, @MappingTarget ExpenseCategory entity);
}

