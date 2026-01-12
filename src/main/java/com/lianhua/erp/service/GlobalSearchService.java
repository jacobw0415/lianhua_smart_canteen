package com.lianhua.erp.service;

import com.lianhua.erp.dto.globalSearch.GlobalSearchRequest;
import com.lianhua.erp.dto.globalSearch.GlobalSearchResponse;

public interface GlobalSearchService {
    GlobalSearchResponse search(GlobalSearchRequest request);
}
