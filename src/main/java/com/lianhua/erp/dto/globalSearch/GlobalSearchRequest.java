package com.lianhua.erp.dto.globalSearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalSearchRequest {

    /**
     * 全域搜尋關鍵字
     */
    private String keyword;

    /**
     * 搜尋範圍
     * orders / purchases / customers
     */
    private List<String> scopes;

    /**
     * 每個模組回傳最大筆數
     */
    private Integer limit;

    /**
     * 每個模組日期塞選區間
     */
    private String period;
}
