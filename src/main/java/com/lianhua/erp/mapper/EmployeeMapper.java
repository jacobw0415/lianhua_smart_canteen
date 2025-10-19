package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.Employee;
import com.lianhua.erp.dto.employee.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {
    
    Employee toEntity(EmployeeRequestDto dto);
    
    EmployeeResponseDto toDto(Employee entity);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(EmployeeRequestDto dto, @MappingTarget Employee entity);
}
