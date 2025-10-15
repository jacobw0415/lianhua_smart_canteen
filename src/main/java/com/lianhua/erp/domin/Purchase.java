package com.lianhua.erp.domin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "purchases")
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @Schema(description = "供應商")
    private Supplier supplier;
    
    @Column(name = "purchase_date", nullable = false)
    @Schema(description = "進貨日期")
    private LocalDate purchaseDate;
    
    @Column(nullable = false, length = 120)
    @Schema(description = "進貨項目")
    private String item;
    
    @Column(nullable = false)
    @Schema(description = "數量")
    private Integer qty;
    
    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    @Schema(description = "單價")
    private BigDecimal unitPrice;
    
    @Column(name = "tax_rate", precision = 5, scale = 2)
    @Schema(description = "稅率（百分比）")
    private BigDecimal taxRate;
    
    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Schema(description = "稅額")
    private BigDecimal taxAmount;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    @Schema(description = "總金額（含稅）")
    private BigDecimal totalAmount;

    // ✅ 新增欄位：已付款金額
    @Column(name = "paid_amount", precision = 10, scale = 2, nullable = false)
    @Schema(description = "已付款金額")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    // ✅ 新增欄位：餘額 (由資料庫計算，不直接寫入)
    @Column(name = "balance", precision = 10, scale = 2, insertable = false, updatable = false)
    @Schema(description = "尚未付款餘額 (自動計算: 總金額 - 已付款)")
    private BigDecimal balance;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Schema(description = "狀態：PENDING, PARTIAL, PAID")
    private Status status = Status.PENDING;
    
    @Column(length = 255)
    @Schema(description = "備註")
    private String note;
    
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "付款紀錄清單")
    @OrderBy("id ASC")
    private Set<Payment> payments = new HashSet<>();
    
    @Column(name = "created_at", updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private java.sql.Timestamp createdAt;
    
    @Column(name = "updated_at", insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private java.sql.Timestamp updatedAt;
    
    public enum Status {
        PENDING, PARTIAL, PAID
    }
}
