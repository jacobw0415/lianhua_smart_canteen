package com.lianhua.erp.mapper;

import com.lianhua.erp.dto.EmployeeDto;
import com.lianhua.erp.domin.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeDto toDto(Employee employee) {
        if (employee == null) return null;

        return EmployeeDto.builder()
                .id(employee.getId())
                .fullName(employee.getFullName())
                .position(employee.getPosition())
                .salary(employee.getSalary())
                .hireDate(employee.getHireDate() != null ? employee.getHireDate().toString() : null)
                .status(employee.getStatus() != null ? employee.getStatus().name() : null)
                .build();
    }

    public Employee toEntity(EmployeeDto dto) {
        if (dto == null) return null;

        Employee employee = new Employee();
        employee.setId(dto.getId());
        employee.setFullName(dto.getFullName());
        employee.setPosition(dto.getPosition());
        employee.setSalary(dto.getSalary());

        if (dto.getHireDate() != null) {
            employee.setHireDate(java.time.LocalDate.parse(dto.getHireDate()));
        }

        if (dto.getStatus() != null) {
            employee.setStatus(Employee.Status.valueOf(dto.getStatus()));
        }

        return employee;
    }
}
