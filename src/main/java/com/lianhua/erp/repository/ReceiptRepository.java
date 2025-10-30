package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    boolean existsByOrderId(Long orderId);
    List<Receipt> findByOrderId(Long orderId);
}
