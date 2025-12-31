package com.lianhua.erp.dto.ar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 應收帳款帳齡查詢條件（AR Aging Filter）
 *
 * 用於：
 * - AR Aging Summary（客戶彙總）
 * - React-Admin List 查詢
 */
@Data
@Schema(description = "應收帳款帳齡查詢條件（AR Aging Filter）")
public class ARAgingFilterDto {

    @Schema(
            description = "客戶名稱（模糊搜尋）",
            example = "立安"
    )
    private String customerName;

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
            description = "是否僅顯示尚未清帳（true = 只顯示未收款）",
            example = "true"
    )
    private Boolean onlyUnpaid;
}

