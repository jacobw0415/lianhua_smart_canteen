package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.EmployeeDto;
import com.lianhua.erp.domin.Employee;
import com.lianhua.erp.mapper.EmployeeMapper;
import com.lianhua.erp.repository.EmployeeRepository;
import com.lianhua.erp.service.EmployeeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public List<EmployeeDto> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(employeeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
        return employeeMapper.toDto(employee);
    }

    @Override
    public EmployeeDto createEmployee(EmployeeDto dto) {
        Employee employee = employeeMapper.toEntity(dto);
        return employeeMapper.toDto(employeeRepository.save(employee));
    }

    @Override
    public EmployeeDto updateEmployee(Long id, EmployeeDto dto) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));

        // 更新欄位
        existing.setFullName(dto.getFullName());
        existing.setPosition(dto.getPosition());
        existing.setSalary(dto.getSalary());
        if (dto.getHireDate() != null) {
            existing.setHireDate(java.time.LocalDate.parse(dto.getHireDate()));
        }
        if (dto.getStatus() != null) {
            existing.setStatus(Employee.Status.valueOf(dto.getStatus()));
        }

        return employeeMapper.toDto(employeeRepository.save(existing));
    }

    @Override
    public void deleteEmployee(Long id) {
        employeeRepository.deleteById(id);
    }
}
