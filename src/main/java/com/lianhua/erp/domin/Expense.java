package com.lianhua.erp.domin;

import jakarta.persistence.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.lianhua.erp.domin.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;

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
    
    @Column(name = "created_at", updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private java.sql.Timestamp createdAt;
    
    @Column(name = "updated_at", updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private java.sql.Timestamp updatedAt;
}
