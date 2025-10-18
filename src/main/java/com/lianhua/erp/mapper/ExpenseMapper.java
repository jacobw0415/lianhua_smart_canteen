package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.*;
import com.lianhua.erp.dto.expense.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {
    
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "employee.fullName", target = "employeeName")
    ExpenseDto toDto(Expense entity);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "employee", ignore = true)
    Expense toEntity(ExpenseRequestDto dto);
}
