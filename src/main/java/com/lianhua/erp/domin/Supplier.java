package com.lianhua.erp.domin;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", length = 10, nullable = false)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "supplier")
    @ToString.Exclude
    @Builder.Default
    private List<Purchase> purchases = new ArrayList<>();

    public enum BillingCycle { WEEKLY, BIWEEKLY, MONTHLY }
}

