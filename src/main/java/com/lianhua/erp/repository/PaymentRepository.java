package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Payment;
import com.lianhua.erp.domain.PaymentRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    
    @Transactional
    void deleteAllByPurchaseId(Long purchaseId);
    
    List<Payment> findByPurchaseId(Long purchaseId);
    void deleteByPurchaseId(Long purchaseId);

    // ⭐ 核心方法：計算進貨單已付金額（排除已作廢的付款）
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.purchase.id = :purchaseId
              AND p.status = :status
        """)
    BigDecimal sumAmountByPurchaseId(@Param("purchaseId") Long purchaseId, @Param("status") PaymentRecordStatus status);

    // ⭐ 檢查進貨單是否有任何付款記錄（包括已作廢的）
    @Query("""
            SELECT COUNT(p) > 0
            FROM Payment p
            WHERE p.purchase.id = :purchaseId
        """)
    boolean hasAnyPaymentByPurchaseId(@Param("purchaseId") Long purchaseId);

    // 查詢有效付款（排除已作廢）
    @Query("SELECT p FROM Payment p WHERE p.purchase.id = :purchaseId AND p.status = :status")
    List<Payment> findActiveByPurchaseId(@Param("purchaseId") Long purchaseId, @Param("status") PaymentRecordStatus status);
}

