package com.lianhua.erp.service;

import com.lianhua.erp.dto.user.RoleDto;
import java.util.List;
import java.util.Set;

public interface RoleService {
    /** 取得所有角色清單 (供管理員查看) */
    List<RoleDto> getAllRoles();

    /** 取得特定角色詳細資訊與其擁有的權限 */
    RoleDto getRoleById(Long id);

    /** 更新角色的權限映射關係 */
    RoleDto updateRolePermissions(Long id, Set<String> permissionNames);
}
