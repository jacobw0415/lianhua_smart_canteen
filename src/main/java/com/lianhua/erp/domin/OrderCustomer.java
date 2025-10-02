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
@Table(name = "order_customers")
public class OrderCustomer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String contactPerson;
    private String phone;
    private String address;

    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle;

    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "customer")
    private List<Order> orders;

    public enum BillingCycle { WEEKLY, BIWEEKLY, MONTHLY }
}

