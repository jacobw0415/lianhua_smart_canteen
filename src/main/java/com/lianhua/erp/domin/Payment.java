package com.lianhua.erp.domin;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "pay_date", nullable = false)
    private LocalDate payDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Method method;
    
    @Column(length = 255)
    private String note;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum Method {
        CASH, TRANSFER, CARD, CHECK
    }
}
