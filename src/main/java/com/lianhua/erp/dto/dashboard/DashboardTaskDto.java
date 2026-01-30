package com.lianhua.erp.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "儀表板：待辦事項與預警明細")
public class DashboardTaskDto {

    @Schema(description = "任務類型", example = "AR_DUE", allowableValues = {"AR_DUE", "ORDER_PENDING"})
    private String type;

    @Schema(description = "相關對象名稱 (客戶或供應商)", example = "蓮華大飯店")
    private String targetName;

    @Schema(description = "相關單據編號", example = "ORD-20260128-001")
    private String referenceNo;

    @Schema(description = "涉及金額 (如欠款餘額)", example = "3450.00")
    private BigDecimal amount;

    @Schema(description = "關鍵日期 (如到期日或預計交貨日)", example = "2026-02-05")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Taipei")
    private LocalDate dueDate;
}