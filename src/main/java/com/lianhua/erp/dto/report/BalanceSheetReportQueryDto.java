package com.lianhua.erp.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 💼 資產負債表查詢條件 DTO
 * 
 * 資產負債表是「時點報表」，查詢截止至指定月底或日期的累積餘額。
 * 用於前端時間選擇器傳遞查詢參數。
 * 
 * 支援多個月份並列比較：
 * - 單一月份：使用 period 參數
 * - 多個月份：使用 periods 參數（逗號分隔或數組）
 * - 單一日期：使用 endDate 參數
 */
@Data
@Schema(description = "資產負債表查詢條件（時點報表）")
public class BalanceSheetReportQueryDto {

    @Schema(
        description = "會計期間（YYYY-MM）\n" +
                     "查詢截止至該月底的累積餘額\n" +
                     "如果提供 periods，則優先使用 periods",
        example = "2025-10"
    )
    private String period;

    @Schema(
        description = "多個會計期間（YYYY-MM）\n" +
                     "查詢多個月份的資產負債表並列比較\n" +
                     "優先使用此參數，如果提供則忽略 period 和 endDate\n" +
                     "支援格式：\n" +
                     "- 數組格式：periods[]=2025-10&periods[]=2025-11&periods[]=2025-12\n" +
                     "- 逗號分隔：periods=2025-10,2025-11,2025-12（Controller 會自動解析）",
        example = "2025-10,2025-11,2025-12"
    )
    private List<String> periods;

    @Schema(
        description = "結束日期（yyyy-MM-dd）\n" +
                     "查詢截止至該日期的累積餘額\n" +
                     "如果 period 或 periods 未提供，則使用此參數",
        example = "2025-12-31"
    )
    private String endDate;

    /**
     * 驗證查詢參數是否有效
     * @return true 如果至少有一個有效的查詢條件
     */
    public boolean hasValidQuery() {
        return (periods != null && !periods.isEmpty()) ||
               (period != null && !period.isBlank()) ||
               (endDate != null && !endDate.isBlank());
    }

    /**
     * 解析 periods 參數（支援逗號分隔字符串）
     * @return periods 列表
     */
    public List<String> getPeriodsList() {
        if (periods != null && !periods.isEmpty()) {
            return periods;
        }
        // 如果 periods 為空但 period 有值，轉換為列表
        if (period != null && !period.isBlank()) {
            return List.of(period);
        }
        return List.of();
    }
}

