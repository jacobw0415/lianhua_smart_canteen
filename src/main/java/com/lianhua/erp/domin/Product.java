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
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Category category;

    private Double unitPrice;
    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product")
    private List<Sale> sales;

    @OneToMany(mappedBy = "product")
    private List<OrderItem> orderItems;

    public enum Category { VEG_LUNCHBOX, SPECIAL, ADD_ON }
}

