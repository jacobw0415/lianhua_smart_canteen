package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {}
