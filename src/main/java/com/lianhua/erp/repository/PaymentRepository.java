package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    @Transactional
    void deleteAllByPurchaseId(Long purchaseId);
    
    List<Payment> findByPurchaseId(Long purchaseId);
    void deleteByPurchaseId(Long purchaseId);
}

