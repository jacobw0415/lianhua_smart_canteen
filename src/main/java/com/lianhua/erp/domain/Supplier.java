package com.lianhua.erp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "suppliers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_supplier_name", columnNames = {"name"})
        }
)
public class Supplier {

    /** 主鍵 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 供應商名稱（唯一） */
    @Column(nullable = false, length = 100)
    private String name;

    /** 聯絡人姓名 */
    @Column(length = 100)
    private String contact;

    /** 聯絡電話 */
    @Column(length = 50)
    private String phone;

    /** 帳單週期 */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", length = 10, nullable = false)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    /** 備註 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String note;

    /** 是否啟用（1=啟用, 0=停用） */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** 建立時間 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新時間 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 關聯的進貨紀錄 */
    @OneToMany(mappedBy = "supplier")
    @ToString.Exclude
    @Builder.Default
    private List<Purchase> purchases = new ArrayList<>();

    public enum BillingCycle { WEEKLY, BIWEEKLY, MONTHLY }
}
