package com.lianhua.erp.dto.expense;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lianhua.erp.domain.ExpenseFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用於回傳費用類別詳細資訊
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "費用類別資料傳輸物件（回傳用）")
public class ExpenseCategoryResponseDto {

    @Schema(description = "費用類別 ID", example = "1")
    private Long id;

    @Schema(description = "費用類別名稱", example = "食材費")
    private String name;

    @Schema(description = "系統自動產生之費用科目代碼（唯讀）", example = "EXP-003", accessMode = Schema.AccessMode.READ_ONLY)
    private String accountCode;

    @Schema(description = "費用說明或備註", example = "每日採購蔬菜、水果支出")
    private String description;

    @Schema(description = "是否啟用此費用類別", example = "true")
    private Boolean active;

    @Schema(description = "是否為薪資類別", example = "false")
    private Boolean isSalary;

    @Schema(description = "費用頻率類型：DAILY（每日一次）、WEEKLY（每週一次）、BIWEEKLY（每兩週一次）、MONTHLY（每月一次）、UNLIMITED（無限制）", example = "MONTHLY")
    private ExpenseFrequency frequencyType;

    @Schema(description = "建立時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime createdAt;

    @Schema(description = "最後更新時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime updatedAt;
}
