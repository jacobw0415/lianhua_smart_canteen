package com.lianhua.erp.domin;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;
    
    @Column(nullable = false)
    private String item;
    
    @Column(nullable = false)
    private Integer qty;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;
    
    private String note;
    
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Payment> payments = new HashSet<>();
    
    public enum Status { PENDING, PARTIAL, PAID }
    
    public void calculateTotals() {
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
        BigDecimal tax = subtotal.multiply(taxRate.divide(BigDecimal.valueOf(100)));
        this.taxAmount = tax;
        this.totalAmount = subtotal.add(tax);
    }
}
