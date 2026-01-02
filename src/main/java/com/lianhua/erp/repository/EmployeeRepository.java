package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    boolean existsByFullName(String fullName);
    
    /**
     * 查詢啟用中的員工（狀態為 ACTIVE），依姓名排序
     * 用於下拉選單等需要顯示可用員工的場景
     */
    List<Employee> findByStatusOrderByFullNameAsc(Employee.Status status);
}
