package com.lianhua.erp.domin;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;
    private LocalDate payDate;

    @Enumerated(EnumType.STRING)
    private Method method;

    private String note;
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    public enum Method { CASH, TRANSFER, CARD, CHECK }
}

