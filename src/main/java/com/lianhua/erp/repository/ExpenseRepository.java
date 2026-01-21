package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Expense;
import com.lianhua.erp.domain.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    /**
     * 檢查薪資類別在同一天是否已有相同類別的支出記錄
     * ✅ 修改：在 SQL 中明確排除 VOIDED 狀態
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee.id = :employeeId
              AND e.expenseDate = :expenseDate
              AND e.category.id = :categoryId
              AND e.status = :status
              AND e.status != 'VOIDED'
            """)
    boolean existsByEmployeeIdAndExpenseDateAndCategoryId(
            @Param("employeeId") Long employeeId,
            @Param("expenseDate") LocalDate expenseDate,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);

    /**
     * 檢查非薪資類別（employee_id 為 NULL）在同一天是否已有相同類別的支出記錄
     * ✅ 修改：在 SQL 中明確排除 VOIDED 狀態
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee IS NULL
              AND e.expenseDate = :expenseDate
              AND e.category.id = :categoryId
              AND e.status = :status
              AND e.status != 'VOIDED'
            """)
    boolean existsByEmployeeIdIsNullAndExpenseDateAndCategoryId(
            @Param("expenseDate") LocalDate expenseDate,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);

    /**
     * 檢查薪資類別在同一會計期間是否已有相同類別的支出記錄
     * ✅ 修改：在 SQL 中明確排除 VOIDED 狀態
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee.id = :employeeId
              AND e.accountingPeriod = :accountingPeriod
              AND e.category.id = :categoryId
              AND e.status = :status
              AND e.status != 'VOIDED'
            """)
    boolean existsByEmployeeIdAndAccountingPeriodAndCategoryId(
            @Param("employeeId") Long employeeId,
            @Param("accountingPeriod") String accountingPeriod,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);

    /**
     * 檢查非薪資類別在同一會計期間是否已有相同類別的支出記錄
     * ✅ 修改：在 SQL 中明確排除 VOIDED 狀態
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee IS NULL
              AND e.accountingPeriod = :accountingPeriod
              AND e.category.id = :categoryId
              AND e.status = :status
              AND e.status != 'VOIDED'
            """)
    boolean existsByEmployeeIdIsNullAndAccountingPeriodAndCategoryId(
            @Param("accountingPeriod") String accountingPeriod,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);

    /**
     * 檢查薪資類別在指定日期區間內是否已有相同類別的支出記錄
     * ✅ 修改：在 SQL 中明確排除 VOIDED 狀態
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee.id = :employeeId
              AND e.expenseDate >= :startDate
              AND e.expenseDate <= :endDate
              AND e.category.id = :categoryId
              AND e.status = :status
              AND e.status != 'VOIDED'
            """)
    boolean existsByEmployeeIdAndExpenseDateBetweenAndCategoryId(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);

    /**
     * 檢查非薪資類別在指定日期區間內是否已有相同類別的支出記錄
     * ✅ 修改：在 SQL 中明確排除 VOIDED 狀態
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee IS NULL
              AND e.expenseDate >= :startDate
              AND e.expenseDate <= :endDate
              AND e.category.id = :categoryId
              AND e.status = :status
              AND e.status != 'VOIDED'
            """)
    boolean existsByEmployeeIdIsNullAndExpenseDateBetweenAndCategoryId(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByEmployeeId(Long employeeId);
}