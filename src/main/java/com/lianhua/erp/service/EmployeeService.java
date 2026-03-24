package com.lianhua.erp.service;

import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.employee.*;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeService {
    /**
     * 查詢所有員工（分頁）
     */
    Page<EmployeeResponseDto> findAll(Pageable pageable);
    
    EmployeeResponseDto findById(Long id);
    EmployeeResponseDto create(EmployeeRequestDto dto);
    EmployeeResponseDto update(Long id, EmployeeRequestDto dto);
    void delete(Long id);
    
    /**
     * 查詢啟用中的員工（狀態為 ACTIVE）
     * 用於下拉選單等需要顯示可用員工的場景
     */
    List<EmployeeResponseDto> getActive();
    
    /**
     * 搜尋員工（支援分頁 + 模糊搜尋）
     */
    Page<EmployeeResponseDto> searchEmployees(EmployeeSearchRequest request, Pageable pageable);

    /**
     * 匯出員工資料（篩選條件與 searchEmployees 相同；scope=all 時匯出全部符合資料）。
     */
    ExportPayload exportEmployees(
            EmployeeSearchRequest request,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    );
    
    /**
     * 啟用員工（將狀態設為 ACTIVE）
     */
    EmployeeResponseDto activate(Long id);
    
    /**
     * 停用員工（將狀態設為 INACTIVE）
     */
    EmployeeResponseDto deactivate(Long id);
}
