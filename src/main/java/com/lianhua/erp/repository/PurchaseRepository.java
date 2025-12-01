package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Purchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {
    @EntityGraph(attributePaths = {"supplier"})
    Optional<Purchase> findWithSupplierById(Long id);

    boolean existsBySupplierId(Long supplierId);

    boolean existsBySupplierIdAndPurchaseDateAndItem(Long supplierId, LocalDate purchaseDate, String item);

    boolean existsBySupplierIdAndPurchaseDateAndItemAndIdNot(Long supplierId, LocalDate purchaseDate, String item, Long id);

}