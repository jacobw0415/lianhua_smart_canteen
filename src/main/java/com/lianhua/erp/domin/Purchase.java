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

    // 🔗 關聯：供應商
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

    // === 稅率與稅額 ===
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    // === 金額相關欄位 ===
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance", precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    // === 狀態與備註 ===
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(length = 255)
    private String note;

    // === 關聯付款紀錄 ===
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Payment> payments = new HashSet<>();

    // === 建立與更新時間 ===
    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING, PARTIAL, PAID
    }

    // === 自動時間維護 ===
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // === 計算總金額（含稅） ===
    public void calculateTotals() {
        if (unitPrice == null || qty == null) {
            this.taxAmount = BigDecimal.ZERO;
            this.totalAmount = BigDecimal.ZERO;
            return;
        }

        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
        BigDecimal rate = taxRate != null ? taxRate : BigDecimal.ZERO;

        BigDecimal tax = rate.compareTo(BigDecimal.ZERO) > 0
                ? subtotal.multiply(rate.divide(BigDecimal.valueOf(100)))
                : BigDecimal.ZERO;

        this.taxAmount = tax.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.totalAmount = subtotal.add(tax).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    // === 加總付款金額 ===
    public BigDecimal getTotalPaid() {
        if (payments == null || payments.isEmpty()) return BigDecimal.ZERO;
        return payments.stream()
                .map(Payment::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // === 自動更新餘額 ===
    public void updateBalance() {
        this.paidAmount = getTotalPaid();
        this.balance = this.totalAmount.subtract(this.paidAmount);
    }
}
