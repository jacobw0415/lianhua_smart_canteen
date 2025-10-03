package com.lianhua.erp.service;

import com.lianhua.erp.dto.EmployeeDto;
import java.util.List;

public interface EmployeeService {
    List<EmployeeDto> getAllEmployees();
    EmployeeDto getEmployeeById(Long id);
    EmployeeDto createEmployee(EmployeeDto dto);
    EmployeeDto updateEmployee(Long id, EmployeeDto dto);
    void deleteEmployee(Long id);
}



