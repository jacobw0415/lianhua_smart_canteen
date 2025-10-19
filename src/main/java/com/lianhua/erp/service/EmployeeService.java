package com.lianhua.erp.service;

import com.lianhua.erp.dto.employee.*;
import java.util.List;

public interface EmployeeService {
    List<EmployeeResponseDto> findAll();
    EmployeeResponseDto findById(Long id);
    EmployeeResponseDto create(EmployeeRequestDto dto);
    EmployeeResponseDto update(Long id, EmployeeRequestDto dto);
    void delete(Long id);
}
