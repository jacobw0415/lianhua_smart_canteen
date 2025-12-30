package com.lianhua.erp.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "purchases", uniqueConstraints = {
        @UniqueConstraint(name = "uk_purchases_purchase_no", columnNames = { "purchase_no" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "進貨主表實體")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "進貨單 ID")
    private Long id;

    @Column(name = "purchase_no", nullable = false, length = 20, updatable = false)
    @Schema(description = "進貨單編號（PO-YYYYMM-XXXX）")
    private String purchaseNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "供應商")
    private Supplier supplier;

    @Column(name = "purchase_date", nullable = false)
    @Schema(description = "進貨日期")
    private LocalDate purchaseDate;

    @Column(name = "accounting_period", length = 7, nullable = false)
    @Schema(description = "會計期間（YYYY-MM）")
    @Builder.Default
    private String accountingPeriod = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    @Schema(description = "總金額（由明細表計算）")
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // ✅ 新增欄位：已付款金額
    @Column(name = "paid_amount", precision = 10, scale = 2, nullable = false)
    @Schema(description = "已付款金額")
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    // ✅ 新增欄位：餘額 (由資料庫計算，不直接寫入)
    @Column(name = "balance", precision = 10, scale = 2, insertable = false, updatable = false)
    @Schema(description = "尚未付款餘額 (自動計算: 總金額 - 已付款)")
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    @Schema(description = "狀態：PENDING, PARTIAL, PAID")
    @Builder.Default
    private Status status = Status.PENDING;

    // 作廢相關欄位（記錄狀態）
    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", length = 20, nullable = false)
    @Builder.Default
    @Schema(description = "記錄狀態：ACTIVE（正常進貨）, VOIDED（已作廢）")
    private PurchaseStatus recordStatus = PurchaseStatus.ACTIVE;

    // 作廢相關欄位（保留作廢時間和原因）
    @Column(name = "voided_at")
    @Schema(description = "作廢時間")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason", length = 500)
    @Schema(description = "作廢原因")
    private String voidReason;

    @Column(length = 255)
    @Schema(description = "備註")
    private String note;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "付款紀錄清單")
    @OrderBy("id ASC")
    @Builder.Default
    private Set<Payment> payments = new HashSet<>();

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "採購明細清單")
    @OrderBy("id ASC")
    @Builder.Default
    private List<PurchaseItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.accountingPeriod == null && this.purchaseDate != null) {
            this.accountingPeriod = this.purchaseDate.format(
                    DateTimeFormatter.ofPattern("yyyy-MM"));
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING, PARTIAL, PAID
    }
}
