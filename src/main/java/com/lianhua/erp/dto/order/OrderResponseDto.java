    package com.lianhua.erp.dto.order;

    import com.fasterxml.jackson.annotation.JsonFormat;
    import com.lianhua.erp.domain.OrderStatus;
    import com.lianhua.erp.domain.PaymentStatus;
    import com.lianhua.erp.dto.orderItem.OrderItemResponseDto;
    import io.swagger.v3.oas.annotations.media.Schema;
    import lombok.*;

    import java.math.BigDecimal;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.List;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "訂單回應 DTO（含明細與狀態資訊）")
    public class OrderResponseDto {

        @Schema(description = "訂單 ID", example = "20251028001")
        private Long id;

        @Schema(description = "訂單編號（商業單號）", example = "SO-202511-0003")
        private String orderNo;

        @Schema(description = "客戶 ID", example = "1001")
        private Long customerId;

        @Schema(description = "客戶名稱", example = "聯華股份有限公司")
        private String customerName;

        @Schema(description = "訂單日期", example = "2025-10-25")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Taipei")
        private LocalDate orderDate;

        @Schema(description = "交貨日期", example = "2025-10-30")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Taipei")
        private LocalDate deliveryDate;

        @Schema(description = "業務狀態", example = "PENDING")
        private OrderStatus orderStatus;

        @Schema(description = "付款狀態", example = "PARTIAL")
        private PaymentStatus paymentStatus;

        @Schema(description = "會計期間", example = "2025-10")
        private String accountingPeriod;

        @Schema(description = "訂單總金額", example = "15800.50")
        private BigDecimal totalAmount;

        @Schema(description = "備註", example = "急件，請優先出貨")
        private String note;

        /* =============================
         * 📌 作廢相關欄位
         * ============================= */

        @Schema(description = "記錄狀態：ACTIVE（正常訂單）, VOIDED（已作廢）", example = "ACTIVE")
        private String recordStatus;

        @Schema(description = "作廢時間")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
        private java.time.LocalDateTime voidedAt;

        @Schema(description = "作廢原因", example = "訂單錯誤，需作廢")
        private String voidReason;

        @Schema(
                description = "訂單明細列表（查詢時回傳）",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        private List<OrderItemResponseDto> items;

        @Schema(description = "建立時間", example = "2025-10-25T09:30:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
        private LocalDateTime createdAt;

        @Schema(description = "更新時間", example = "2025-10-27T14:45:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
        private LocalDateTime updatedAt;
    }
