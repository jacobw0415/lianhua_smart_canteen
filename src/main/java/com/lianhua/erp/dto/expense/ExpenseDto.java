package com.lianhua.erp.dto.expense;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lianhua.erp.domain.ExpenseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用於回傳支出紀錄詳細資料
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "支出資料傳輸物件（回傳用）")
public class ExpenseDto {
    
    @Schema(description = "支出 ID", example = "101")
    private Long id;
    
    @Schema(description = "支出日期", example = "2025-10-01")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;
    
    @Schema(description = "會計期間（YYYY-MM）", example = "2025-10")
    private String accountingPeriod;
    
    @Schema(description = "費用類別名稱", example = "食材費")
    private String categoryName;
    
    @Schema(description = "支出金額（新台幣）", example = "3500.00")
    private BigDecimal amount;
    
    @Schema(description = "支出備註", example = "進貨青菜、水果等")
    private String note;
    
    @Schema(description = "員工名稱（若支出與員工薪資相關）", example = "王小明")
    private String employeeName;
    
    @Schema(description = "狀態：ACTIVE（正常支出）, VOIDED（已作廢）", example = "ACTIVE")
    private ExpenseStatus status;
    
    @Schema(description = "作廢時間")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime voidedAt;
    
    @Schema(description = "作廢原因", example = "輸入錯誤")
    private String voidReason;
}
