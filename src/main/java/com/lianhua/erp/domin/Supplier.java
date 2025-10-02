package com.lianhua.erp.domin;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String contact;
    private String phone;

    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle;

    @Lob
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "supplier")
    private List<Purchase> purchases;

    public enum BillingCycle { WEEKLY, BIWEEKLY, MONTHLY }
}

