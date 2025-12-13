package com.lianhua.erp.numbering;

import com.lianhua.erp.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PurchaseNoGenerator {
    
    private static final DateTimeFormatter YM_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMM");
    
    private final PurchaseRepository purchaseRepository;
    
    /**
     * 產生進貨單編號
     * 規則：PO-YYYYMM-XXXX
     */
    @Transactional
    public String generate(LocalDate purchaseDate) {
        
        if (purchaseDate == null) {
            throw new IllegalArgumentException("進貨日期不可為空");
        }
        
        String ym = purchaseDate.format(YM_FORMAT);
        String prefix = "PO-" + ym;
        
        int nextSeq = purchaseRepository.findNextPurchaseSequence(prefix);
        
        return prefix + "-" + String.format("%04d", nextSeq);
    }
}