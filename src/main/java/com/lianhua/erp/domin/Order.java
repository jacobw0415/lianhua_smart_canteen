package com.lianhua.erp.domin;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate orderDate;
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Double totalAmount;
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private OrderCustomer customer;

    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;

    public enum Status { PENDING, CONFIRMED, DELIVERED, CANCELLED, PAID }
}
