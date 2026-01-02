package com.lianhua.erp.repository;

import com.lianhua.erp.domain.ExpenseCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long>, JpaSpecificationExecutor<ExpenseCategory> {
    boolean existsByName(String name);
    Optional<ExpenseCategory> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    
    /**
     * 檢查名稱是否已存在（排除指定 ID，用於更新時檢查）
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    
    List<ExpenseCategory> findAllByOrderByAccountCodeAsc();
    List<ExpenseCategory> findAllByActiveTrueOrderByAccountCodeAsc();
    
    /**
     * 使用 Spring Data JPA 方法名查詢啟用的薪資類別（更可靠的方式）
     */
    List<ExpenseCategory> findByActiveTrueAndIsSalaryTrueOrderByNameAsc();
    
    /**
     * 查找最大的會計代碼（用於生成新代碼）
     * 使用悲观锁防止并发问题
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM ExpenseCategory e ORDER BY e.accountCode DESC")
    List<ExpenseCategory> findAllOrderByAccountCodeDesc();
    
    /**
     * 獲取所有費用類別的名稱（用於相似性檢查）
     */
    @Query("SELECT e.name FROM ExpenseCategory e")
    List<String> findAllNames();
    
    /**
     * 獲取所有費用類別的名稱（排除指定 ID，用於更新時檢查）
     */
    @Query("SELECT e.name FROM ExpenseCategory e WHERE e.id != :excludeId")
    List<String> findAllNamesExcludingId(Long excludeId);
    
    /**
     * 查找所有啟用的薪資類別（使用 isSalary 字段）
     * 推薦使用此方法替代 findActiveByNameContaining("薪資")
     */
    @Query("SELECT e FROM ExpenseCategory e WHERE e.active = true AND e.isSalary = true ORDER BY e.name ASC")
    List<ExpenseCategory> findActiveSalaryCategories();
    
    /**
     * 查找名稱包含指定關鍵字的啟用中費用類別
     * @deprecated 已廢棄，請使用 findActiveSalaryCategories() 代替
     * 此方法保留僅用於向後兼容，不建議使用
     */
    @Deprecated
    @Query("SELECT e FROM ExpenseCategory e WHERE e.active = true AND LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY e.name ASC")
    List<ExpenseCategory> findActiveByNameContaining(String keyword);
}

