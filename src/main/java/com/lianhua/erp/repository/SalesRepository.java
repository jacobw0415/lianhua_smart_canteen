package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesRepository extends JpaRepository<Sale, Long> {
    
    List<Sale> findByProductId(Long productId);
    
    boolean existsBySaleDateAndProductId(LocalDate saleDate, Long productId);
}
