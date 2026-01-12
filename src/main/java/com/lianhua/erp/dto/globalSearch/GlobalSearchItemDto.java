package com.lianhua.erp.dto.globalSearch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GlobalSearchItemDto {

    /**
     * ORDER / PURCHASE / CUSTOMER
     */
    private String type;

    private Long id;

    /**
     * 主顯示文字（例如單號、名稱）
     */
    private String title;

    /**
     * 次要顯示文字（例如客戶名、供應商）
     */
    private String subtitle;

    /**
     * 狀態（ACTIVE / VOIDED 等）
     */
    private String status;

    /**
     * 前端導頁路徑
     */
    private String route;
}
