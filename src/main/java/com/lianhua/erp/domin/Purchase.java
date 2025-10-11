package com.lianhua.erp.domin;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purchase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // --- 關聯到 Supplier ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    
    // --- 關聯到 Payment ---
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Payment> payments = new HashSet<>();
    
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;
    
    @Column(nullable = false, length = 120)
    private String item;
    
    @Column(nullable = false)
    private Integer qty;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;
    
    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
    
    public enum Status {
        PENDING, PAID, PARTIAL
    }
    
    // --- 輔助方法 ---
    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setPurchase(this);
    }
    
    public void removePayment(Payment payment) {
        payments.remove(payment);
        payment.setPurchase(null);
    }
}
