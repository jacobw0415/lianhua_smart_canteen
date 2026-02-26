package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Permission;
import com.lianhua.erp.domain.Role;
import com.lianhua.erp.dto.user.RoleDto;
import com.lianhua.erp.repository.PermissionRepository;
import com.lianhua.erp.repository.RoleRepository;
import com.lianhua.erp.service.RoleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /** 僅回傳 name 以 ROLE_ 開頭的角色，避免歷史資料中的 ADMIN/USER 等非標準項出現在 Swagger/前端 */
    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .filter(r -> r.getName() != null && r.getName().startsWith("ROLE_"))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getRoleById(Long id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到角色 ID: " + id));
        return convertToDto(role);
    }

    @Override
    @Transactional
    public RoleDto updateRolePermissions(Long id, Set<String> permissionNames) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("角色不存在"));

        // 根據權限名稱查找對應的 Permission Entity 清單
        Set<Permission> newPermissions = permissionNames.stream()
                .map(name -> permissionRepository.findByName(name)
                        .orElseThrow(() -> new EntityNotFoundException("權限不存在: " + name)))
                .collect(Collectors.toSet());

        // 更新關聯表 (role_permissions)
        role.setPermissions(newPermissions);
        return convertToDto(roleRepository.save(role));
    }

    private RoleDto convertToDto(Role role) {
        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .displayName(role.getDescription())
                .permissions(role.getPermissions().stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
}