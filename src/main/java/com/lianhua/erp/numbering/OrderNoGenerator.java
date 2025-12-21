package com.lianhua.erp.numbering;

import com.lianhua.erp.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OrderNoGenerator {
    
    private static final DateTimeFormatter YM_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMM");
    
    private final OrderRepository orderRepository;
    
    /**
     * 產生訂單編號
     * 規則：SO-YYYYMM-XXXX
     * SO = Sales Order（銷售訂單）
     */
    @Transactional
    public String generate(LocalDate orderDate) {
        
        if (orderDate == null) {
            throw new IllegalArgumentException("訂單日期不可為空");
        }
        
        String ym = orderDate.format(YM_FORMAT);
        String prefix = "SO-" + ym;
        
        int nextSeq = orderRepository.findNextOrderSequence(prefix);
        
        return prefix + "-" + String.format("%04d", nextSeq);
    }
}

