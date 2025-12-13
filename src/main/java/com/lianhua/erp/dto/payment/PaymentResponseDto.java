package com.lianhua.erp.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "付款紀錄回應資料")
public class PaymentResponseDto {

    /* =============================
     * 📌 基本付款資訊
     * ============================= */

    @Schema(description = "付款紀錄 ID", example = "50")
    private Long id;

    @Schema(description = "對應的進貨單 ID", example = "10")
    private Long purchaseId;
    
    @Schema(description = "對應的進貨單編號（商業單號）", example = "PO-202512-0007")
    private String purchaseNo;

    @Schema(description = "付款金額", example = "1200.00")
    private BigDecimal amount;

    @Schema(description = "付款日期", example = "2025-10-12")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate payDate;

    @Schema(description = "付款方式", example = "TRANSFER")
    private String method;

    @Schema(description = "付款參考號碼", example = "TXN-20251012-001")
    private String referenceNo;

    @Schema(description = "付款備註", example = "已付款一半")
    private String note;


    /* =============================
     * 📌 PaymentList 顯示需要的欄位
     * ============================= */

    @Schema(description = "供應商名稱", example = "泰山蔬果供應行")
    private String supplierName;   // 付款列表必須顯示供應商

    @Schema(description = "品項摘要", example = "青江菜 50kg")
    private String item;           // 付款列表需要知道付款屬於哪筆進貨

    @Schema(description = "會計期間 (YYYY-MM)", example = "2025-12")
    private String accountingPeriod; // 用於月份報表與查詢
}
