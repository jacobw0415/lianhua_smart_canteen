package com.lianhua.erp.domin;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "expense_categories",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "name"),
                @UniqueConstraint(columnNames = "account_code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "費用類別主檔（支援階層式分類與自動會計代碼）")
public class ExpenseCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "費用類別 ID")
    private Long id;
    
    @Column(nullable = false, length = 100, unique = true)
    @Schema(description = "類別名稱", example = "食材費")
    private String name;
    
    @Column(name = "account_code", unique = true, length = 20, nullable = false)
    @Schema(description = "系統自動產生的會計科目代碼（唯讀）", example = "EXP-003", accessMode = Schema.AccessMode.READ_ONLY)
    private String accountCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @Schema(description = "上層分類（可為空）")
    private ExpenseCategory parent;
    
    @Column(length = 255)
    @Schema(description = "說明", example = "此分類屬於食材相關成本")
    private String description;
    
    @Column(nullable = false)
    @Builder.Default
    @Schema(description = "是否啟用", example = "true")
    private Boolean active = true;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Schema(description = "建立時間（系統自動生成）", example = "2025-10-16T10:30:00")
    private java.sql.Timestamp createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Schema(description = "最後更新時間（系統自動更新）", example = "2025-10-16T11:05:00")
    private java.sql.Timestamp updatedAt;
}
