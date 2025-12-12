package com.lianhua.erp.dto.ap;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 應付帳款帳齡查詢條件（AP Aging Filter）
 *
 * 用於：
 * - AP Aging Summary（供應商彙總）
 * - React-Admin List 查詢
 */
@Data
@Schema(description = "應付帳款帳齡查詢條件（AP Aging Filter）")
public class APAgingFilterDto {

    @Schema(
            description = "供應商名稱（模糊搜尋）",
            example = "明亮"
    )
    private String supplierName;

    @Schema(
            description = """
            帳齡區間：
            - ALL：全部
            - DAYS_0_30：0–30 天
            - DAYS_31_60：31–60 天
            - DAYS_60_PLUS：60 天以上
            """,
            example = "DAYS_0_30",
            allowableValues = {
                    "ALL",
                    "DAYS_0_30",
                    "DAYS_31_60",
                    "DAYS_60_PLUS"
            }
    )
    private String agingBucket;

    @Schema(
            description = "是否僅顯示尚未清帳（true = 只顯示未付款）",
            example = "true"
    )
    private Boolean onlyUnpaid;
}