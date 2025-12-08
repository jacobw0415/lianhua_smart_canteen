package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    
    @Transactional
    void deleteAllByPurchaseId(Long purchaseId);
    
    List<Payment> findByPurchaseId(Long purchaseId);
    void deleteByPurchaseId(Long purchaseId);
}

