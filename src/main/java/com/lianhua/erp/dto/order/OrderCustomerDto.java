package com.lianhua.erp.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "訂單商家 DTO")
public class OrderCustomerDto {
    @Schema(description = "商家 ID", example = "101")
    private Long id;

    @Schema(description = "名稱", example = "台北某國中")
    private String name;

    @Schema(description = "聯絡人", example = "張老師")
    private String contactPerson;

    @Schema(description = "電話", example = "0223456789")
    private String phone;

    @Schema(description = "地址", example = "台北市中正區XX路100號")
    private String address;

    @Schema(description = "結帳週期", example = "MONTHLY")
    private String billingCycle;

    @Schema(description = "備註", example = "每週一、三送餐")
    private String note;
}
