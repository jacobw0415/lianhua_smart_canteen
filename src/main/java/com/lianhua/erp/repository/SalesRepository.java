package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {
    
    List<Sale> findByProductId(Long productId);
    
    boolean existsBySaleDateAndProductId(LocalDate saleDate, Long productId);

    boolean existsByProductId(Long productId);

    boolean existsBySaleDate(LocalDate saleDate);
}
