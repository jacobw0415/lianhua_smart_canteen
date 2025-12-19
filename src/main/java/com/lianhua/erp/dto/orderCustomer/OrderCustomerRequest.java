package com.lianhua.erp.dto.orderCustomer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "客戶請求 DTO（新增 / 編輯用）")
public class OrderCustomerRequest {

    @Schema(description = "客戶名稱", example = "聯華股份有限公司", required = true)
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
}
