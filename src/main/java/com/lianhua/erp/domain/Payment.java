package com.lianhua.erp.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_reference_no", columnNames = {"reference_no"})
        }
)
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
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "pay_date")
    private LocalDate payDate = LocalDate.now();
    
    @Column(name = "accounting_period", length = 7, nullable = false)
    @Schema(description = "會計期間（YYYY-MM）")
    private String accountingPeriod = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
    
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 10)
    private Method method = Method.CASH;

    @Column(name = "reference_no", length = 100, unique = false)
    private String referenceNo;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.accountingPeriod == null && this.payDate != null) {
            this.accountingPeriod = this.payDate.format(
                    DateTimeFormatter.ofPattern("yyyy-MM")
            );
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Method { CASH, TRANSFER, CARD, CHECK }
}
