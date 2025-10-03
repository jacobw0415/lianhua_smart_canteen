package com.lianhua.erp.mapper;

import com.lianhua.erp.dto.ExpenseDto;
import com.lianhua.erp.domin.Employee;
import com.lianhua.erp.domin.Expense;
import com.lianhua.erp.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {

    private final EmployeeRepository employeeRepository;

    public ExpenseMapper(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public ExpenseDto toDto(Expense expense) {
        if (expense == null) return null;

        return ExpenseDto.builder()
                .id(expense.getId())
                .expenseDate(expense.getExpenseDate().toString())
                .category(expense.getCategory())
                .amount(expense.getAmount())
                .note(expense.getNote())
                .employeeId(expense.getEmployee() != null ? expense.getEmployee().getId() : null)
                .build();
    }

    public Expense toEntity(ExpenseDto dto) {
        if (dto == null) return null;

        Expense expense = new Expense();
        expense.setId(dto.getId());
        expense.setExpenseDate(java.time.LocalDate.parse(dto.getExpenseDate()));
        expense.setCategory(dto.getCategory());
        expense.setAmount(dto.getAmount());
        expense.setNote(dto.getNote());

        if (dto.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found: " + dto.getEmployeeId()));
            expense.setEmployee(employee);
        }

        return expense;
    }
}
