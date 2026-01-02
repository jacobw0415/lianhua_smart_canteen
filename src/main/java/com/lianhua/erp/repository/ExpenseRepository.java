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
     * 只檢查 ACTIVE 狀態的記錄（已作廢的記錄不參與頻率檢查）
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee.id = :employeeId
              AND e.expenseDate = :expenseDate
              AND e.category.id = :categoryId
              AND e.status = :status
            """)
    boolean existsByEmployeeIdAndExpenseDateAndCategoryId(
            @Param("employeeId") Long employeeId,
            @Param("expenseDate") LocalDate expenseDate,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);
    
    /**
     * 檢查非薪資類別（employee_id 為 NULL）在同一天是否已有相同類別的支出記錄
     * 用於防止同一天建立多筆相同類別的非薪資支出（如房租）
     * 只檢查 ACTIVE 狀態的記錄（已作廢的記錄不參與頻率檢查）
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee IS NULL
              AND e.expenseDate = :expenseDate
              AND e.category.id = :categoryId
              AND e.status = :status
            """)
    boolean existsByEmployeeIdIsNullAndExpenseDateAndCategoryId(
            @Param("expenseDate") LocalDate expenseDate,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);
    
    /**
     * 檢查薪資類別在同一會計期間是否已有相同類別的支出記錄
     * 用於頻率類型為 MONTHLY 的費用類別
     * 只檢查 ACTIVE 狀態的記錄（已作廢的記錄不參與頻率檢查）
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee.id = :employeeId
              AND e.accountingPeriod = :accountingPeriod
              AND e.category.id = :categoryId
              AND e.status = :status
            """)
    boolean existsByEmployeeIdAndAccountingPeriodAndCategoryId(
            @Param("employeeId") Long employeeId,
            @Param("accountingPeriod") String accountingPeriod,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);
    
    /**
     * 檢查非薪資類別在同一會計期間是否已有相同類別的支出記錄
     * 用於頻率類型為 MONTHLY 的費用類別（如房租）
     * 只檢查 ACTIVE 狀態的記錄（已作廢的記錄不參與頻率檢查）
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee IS NULL
              AND e.accountingPeriod = :accountingPeriod
              AND e.category.id = :categoryId
              AND e.status = :status
            """)
    boolean existsByEmployeeIdIsNullAndAccountingPeriodAndCategoryId(
            @Param("accountingPeriod") String accountingPeriod,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);
    
    /**
     * 檢查薪資類別在指定日期區間內是否已有相同類別的支出記錄
     * 用於頻率類型為 WEEKLY 或 BIWEEKLY 的費用類別
     * 只檢查 ACTIVE 狀態的記錄（已作廢的記錄不參與頻率檢查）
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee.id = :employeeId
              AND e.expenseDate >= :startDate
              AND e.expenseDate <= :endDate
              AND e.category.id = :categoryId
              AND e.status = :status
            """)
    boolean existsByEmployeeIdAndExpenseDateBetweenAndCategoryId(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);
    
    /**
     * 檢查非薪資類別在指定日期區間內是否已有相同類別的支出記錄
     * 用於頻率類型為 WEEKLY 或 BIWEEKLY 的費用類別
     * 只檢查 ACTIVE 狀態的記錄（已作廢的記錄不參與頻率檢查）
     */
    @Query("""
            SELECT COUNT(e) > 0
            FROM Expense e
            WHERE e.employee IS NULL
              AND e.expenseDate >= :startDate
              AND e.expenseDate <= :endDate
              AND e.category.id = :categoryId
              AND e.status = :status
            """)
    boolean existsByEmployeeIdIsNullAndExpenseDateBetweenAndCategoryId(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("status") ExpenseStatus status);
    
    boolean existsByCategoryId(Long categoryId);
    
    /**
     * 檢查指定員工是否被支出記錄引用
     * 用於刪除員工前的檢查
     */
    boolean existsByEmployeeId(Long employeeId);
}
