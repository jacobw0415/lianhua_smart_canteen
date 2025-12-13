package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Purchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface PurchaseRepository
        extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {
    
    @EntityGraph(attributePaths = {"supplier"})
    Optional<Purchase> findWithSupplierById(Long id);
    
    boolean existsBySupplierId(Long supplierId);
    
    boolean existsBySupplierIdAndPurchaseDateAndItem(
            Long supplierId, LocalDate purchaseDate, String item);
    
    boolean existsBySupplierIdAndPurchaseDateAndItemAndIdNot(
            Long supplierId, LocalDate purchaseDate, String item, Long id);
    
    Optional<Purchase> findByPurchaseNo(String purchaseNo);
    
    boolean existsByPurchaseNo(String purchaseNo);
    
    @Query("""
            SELECT COALESCE(
                MAX(
                    CAST(
                        SUBSTRING(p.purchaseNo, LENGTH(:prefix) + 2)
                        AS integer
                    )
                ),
                0
            ) + 1
            FROM Purchase p
            WHERE p.purchaseNo LIKE CONCAT(:prefix, '-%')
            """)
    int findNextPurchaseSequence(@Param("prefix") String prefix);
}