package com.lianhua.erp.repository;

import com.lianhua.erp.domain.PurchaseItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate; // ✨ 需新增此 import
import java.util.List;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    List<PurchaseItem> findByPurchaseId(Long purchaseId);

    void deleteByPurchaseId(Long purchaseId);

    /**
     * ✅ 新增：檢查是否存在有效的進貨品項（排除已作廢的進貨單）
     */
    @Query("""
            SELECT COUNT(i) > 0
            FROM PurchaseItem i
            WHERE i.purchase.supplier.id = :supplierId
              AND i.purchase.purchaseDate = :purchaseDate
              AND i.item = :itemName
              AND i.purchase.recordStatus != 'VOIDED'
            """)
    boolean existsActivePurchaseItem(
            @Param("supplierId") Long supplierId,
            @Param("purchaseDate") LocalDate purchaseDate,
            @Param("itemName") String itemName);

    @Query("SELECT COALESCE(SUM(i.subtotal), 0) FROM PurchaseItem i WHERE i.purchase.id = :purchaseId")
    BigDecimal sumTotalByPurchaseId(@Param("purchaseId") Long purchaseId);

    @Query("""
            SELECT i FROM PurchaseItem i
            WHERE LOWER(i.item) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(i.note) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<PurchaseItem> search(@Param("keyword") String keyword, Pageable pageable);
}