package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {}