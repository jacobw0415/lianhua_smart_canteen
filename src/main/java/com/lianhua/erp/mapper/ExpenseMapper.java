package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.dto.expense.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {
    
    @Mappings({
        @Mapping(source = "category.name", target = "categoryName"),
        @Mapping(source = "employee.fullName", target = "employeeName"),
        @Mapping(source = "status", target = "status"),
        @Mapping(source = "voidedAt", target = "voidedAt"),
        @Mapping(source = "voidReason", target = "voidReason")
    })
    ExpenseDto toDto(Expense entity);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "voidedAt", ignore = true)
    @Mapping(target = "voidReason", ignore = true)
    Expense toEntity(ExpenseRequestDto dto);
}
