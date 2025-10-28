package com.lianhua.erp.dto.orderCustomer;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "客戶回應 DTO")
public class OrderCustomerResponseDto {
    
    @Schema(description = "ID", example = "1001")
    private Long id;
    
    @Schema(description = "客戶名稱", example = "聯華股份有限公司")
    private String name;
    
    @Schema(description = "聯絡人", example = "王小明")
    private String contactPerson;
    
    @Schema(description = "電話", example = "02-1234-5678")
    private String phone;
    
    @Schema(description = "地址", example = "台北市中山區南京東路三段100號10樓")
    private String address;
    
    @Schema(description = "結帳週期", example = "每月月底結帳")
    private String billingCycle;
    
    @Schema(description = "備註", example = "重要客戶，需提前提醒付款")
    private String note;
    
    @Schema(description = "建立時間",
            example = "2025-01-15 09:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新時間",
            example = "2025-02-10 14:45:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime updatedAt;
}
