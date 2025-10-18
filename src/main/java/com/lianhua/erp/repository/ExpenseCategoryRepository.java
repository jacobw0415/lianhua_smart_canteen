package com.lianhua.erp.repository;

import com.lianhua.erp.domin.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    boolean existsByName(String name);
    Optional<ExpenseCategory> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<ExpenseCategory> findAllByOrderByAccountCodeAsc();
    List<ExpenseCategory> findAllByActiveTrueOrderByAccountCodeAsc();
    Optional<ExpenseCategory> findTopByParentOrderByAccountCodeDesc(ExpenseCategory parent);
    @Query("SELECT e FROM ExpenseCategory e WHERE e.parent IS NULL ORDER BY e.accountCode DESC LIMIT 1")
    Optional<ExpenseCategory> findTopLevelLastCode();
    
    long countByParent(ExpenseCategory parent);
}

