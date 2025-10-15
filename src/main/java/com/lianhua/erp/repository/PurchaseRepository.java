package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Purchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    @EntityGraph(attributePaths = {"supplier"})
    Optional<Purchase> findWithSupplierById(Long id);
}