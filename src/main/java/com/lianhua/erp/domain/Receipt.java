package com.lianhua.erp.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "receipts",
        indexes = {
                @Index(name = "idx_receipts_accounting_period", columnList = "accounting_period"),
                @Index(name = "idx_receipts_status", columnList = "status"),
                @Index(name = "idx_receipts_order_id", columnList = "order_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 關聯訂單
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_order"))
    private Order order;
    
    private LocalDate receivedDate = LocalDate.now();
    
    @Column(name = "accounting_period", length = 7, nullable = false)
    private String accountingPeriod;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount; // ✅ 將由 Service 自動設定
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod method = PaymentMethod.CASH;
    
    @Column(name = "reference_no", length = 100)
    private String referenceNo;
    
    @Column(length = 255)
    private String note;
    
    // 狀態欄位
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private ReceiptStatus status = ReceiptStatus.ACTIVE;
    
    // 作廢相關欄位（保留作廢時間和原因）
    @Column(name = "voided_at")
    private LocalDateTime voidedAt;
    
    @Column(name = "void_reason", length = 500)
    private String voidReason;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        if (this.accountingPeriod == null && this.receivedDate != null) {
            this.accountingPeriod = this.receivedDate.format(
                    DateTimeFormatter.ofPattern("yyyy-MM")
            );
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum PaymentMethod {
        CASH, TRANSFER, CARD, CHECK
    }
}
