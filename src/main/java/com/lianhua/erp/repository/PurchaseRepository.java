package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Purchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    @EntityGraph(attributePaths = {"supplier"})
    Optional<Purchase> findWithSupplierById(Long id);

    boolean existsBySupplierIdAndPurchaseDateAndItem(Long supplierId, LocalDate purchaseDate, String item);

    boolean existsBySupplierIdAndPurchaseDateAndItemAndIdNot(Long supplierId, LocalDate purchaseDate, String item, Long id);

}