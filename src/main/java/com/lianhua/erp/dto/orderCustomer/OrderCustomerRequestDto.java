package com.lianhua.erp.dto.orderCustomer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "客戶建立或更新請求 DTO")
public class OrderCustomerRequestDto {
    
    @NotBlank
    @Size(max = 120)
    @Schema(description = "客戶名稱", example = "蓮華素食")
    private String name;
    
    @Schema(description = "聯絡人", example = "王小姐")
    private String contactPerson;
    
    @Schema(description = "電話", example = "0912-345-678")
    private String phone;
    
    @Schema(description = "地址", example = "台北市中山區南京東路三段123號")
    private String address;
    
    @Schema(description = "結帳週期", example = "MONTHLY", allowableValues = {"WEEKLY", "BIWEEKLY", "MONTHLY"})
    private String billingCycle;
    
    @Schema(description = "備註", example = "每月月底結帳")
    private String note;
}
