package com.lianhua.erp.service;

import com.lianhua.erp.dto.user.PermissionDto;

import java.util.List;

/**
 * 權限查詢服務
 * 供管理員取得權限清單，用於角色權限矩陣 UI 或 @PreAuthorize hasAuthority 對照
 */
public interface PermissionService {

    /** 取得所有權限定義（依模組可選） */
    List<PermissionDto> getAllPermissions(String module);
}
