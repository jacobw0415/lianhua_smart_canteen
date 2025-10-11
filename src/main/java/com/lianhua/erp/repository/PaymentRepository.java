package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    @Transactional
    void deleteAllByPurchaseId(Long purchaseId);
}
