package com.lianhua.erp.domain;

import jakarta.persistence.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "expenses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "expense_date", "category_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "開支主表")
public class Expense {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "支出 ID")
    private Long id;
    
    @Column(name = "expense_date", nullable = false)
    @Schema(description = "支出日期")
    private LocalDate expenseDate;
    
    @Column(name = "accounting_period", length = 7, nullable = false)
    @Schema(description = "會計期間（YYYY-MM）")
    private String accountingPeriod = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @Schema(description = "費用類別")
    private ExpenseCategory category;
    
    @Column(nullable = false, precision = 10, scale = 2)
    @Schema(description = "支出金額")
    private BigDecimal amount;
    
    @Column(length = 255)
    @Schema(description = "備註")
    private String note;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @Schema(description = "對應員工（如為薪資支出）")
    private Employee employee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.accountingPeriod == null && this.expenseDate != null) {
            this.accountingPeriod = this.expenseDate.format(
                    DateTimeFormatter.ofPattern("yyyy-MM")
            );
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
