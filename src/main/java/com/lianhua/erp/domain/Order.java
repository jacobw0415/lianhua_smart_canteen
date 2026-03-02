package com.lianhua.erp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_orders_order_no", columnNames = { "order_no" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 20, updatable = false)
    private String orderNo;

    // 訂單客戶
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private OrderCustomer customer;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "accounting_period", nullable = false, length = 7)
    private String accountingPeriod;

    // 業務狀態（物流 / 訂單流程）
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    // 付款狀態
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20) 
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    // 🚀 關鍵新增：同步 Schema v2.8 的作廢欄位
    // 這些欄位讓 Order 一次性帶出作廢資訊，徹底解決前端閃跳
    @Column(name = "record_status", nullable = false, length = 20)
    @Builder.Default
    private String recordStatus = "ACTIVE"; // ACTIVE or VOIDED

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

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

        // 預設記錄狀態為正常
        if (this.recordStatus == null) {
            this.recordStatus = "ACTIVE";
        }

        if (this.accountingPeriod == null && this.deliveryDate != null) {
            this.accountingPeriod = this.deliveryDate.format(
                    DateTimeFormatter.ofPattern("yyyy-MM"));
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * 便利方法：判斷是否已作廢
     */
    public boolean isVoided() {
        return "VOIDED".equals(this.recordStatus);
    }
}