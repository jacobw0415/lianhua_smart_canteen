package com.lianhua.erp.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "sales",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sale_date", "product_id"})
)
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;
    
    @Column(name = "accounting_period", length = 7, nullable = false)
    @Schema(description = "會計期間（YYYY-MM）")
    private String accountingPeriod = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
    
    @Column(nullable = false)
    private Integer qty;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_method", nullable = false, length = 20)
    private PayMethod payMethod;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.accountingPeriod == null && this.saleDate != null) {
            this.accountingPeriod = this.saleDate.format(
                    DateTimeFormatter.ofPattern("yyyy-MM")
            );
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public enum PayMethod { CASH, CARD, MOBILE }
}

