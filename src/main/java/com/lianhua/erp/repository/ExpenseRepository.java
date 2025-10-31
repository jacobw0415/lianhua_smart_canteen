package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    boolean existsByEmployeeIdAndExpenseDateAndCategoryId(Long employeeId, java.time.LocalDate date, Long categoryId);
}
