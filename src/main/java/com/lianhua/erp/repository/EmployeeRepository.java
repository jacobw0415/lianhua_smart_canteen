package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByFullName(String fullName);
}
