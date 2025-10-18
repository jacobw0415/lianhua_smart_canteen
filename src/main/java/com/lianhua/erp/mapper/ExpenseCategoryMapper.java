package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.ExpenseCategory;
import com.lianhua.erp.dto.expense.ExpenseCategoryDto;
import com.lianhua.erp.dto.expense.ExpenseCategoryRequestDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ExpenseCategoryMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountCode", ignore = true)
    ExpenseCategory toEntity(ExpenseCategoryRequestDto dto);
    
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    ExpenseCategoryDto toDto(ExpenseCategory entity);
    
    @Mapping(target = "accountCode", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(ExpenseCategoryRequestDto dto, @MappingTarget ExpenseCategory entity);
}

