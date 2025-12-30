package com.lianhua.erp.repository;

import com.lianhua.erp.domain.PurchaseItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {
    
    List<PurchaseItem> findByPurchaseId(Long purchaseId);
    
    void deleteByPurchaseId(Long purchaseId);
    
    /**
     * 計算指定採購單的所有明細總金額
     */
    @Query("SELECT COALESCE(SUM(i.subtotal), 0) FROM PurchaseItem i WHERE i.purchase.id = :purchaseId")
    BigDecimal sumTotalByPurchaseId(@Param("purchaseId") Long purchaseId);
    
    /**
     * 支援關鍵字搜尋（搜尋品項名稱或備註）
     */
    @Query("""
            SELECT i FROM PurchaseItem i
            WHERE LOWER(i.item) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(i.note) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<PurchaseItem> search(@Param("keyword") String keyword, Pageable pageable);
}

