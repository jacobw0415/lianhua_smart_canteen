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
    
    @Schema(description = "ID")
    private Long id;
    
    @Schema(description = "客戶名稱")
    private String name;
    
    @Schema(description = "聯絡人")
    private String contactPerson;
    
    @Schema(description = "電話")
    private String phone;
    
    @Schema(description = "地址")
    private String address;
    
    @Schema(description = "結帳週期")
    private String billingCycle;
    
    @Schema(description = "備註")
    private String note;
    
    @Schema(description = "建立時間")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新時間")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
    private LocalDateTime updatedAt;
}
