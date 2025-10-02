package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {}
