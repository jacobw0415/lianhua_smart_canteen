package com.lianhua.erp.dto.globalSearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor // ✅ 務必加上，Jackson 序列化建議具備
public class GlobalSearchResponse {

    // ✅ 初始化為空的 ArrayList，避免 Service 忘記給值時出現 Null
    @Builder.Default
    private List<GlobalSearchItemDto> items = new ArrayList<>();
}