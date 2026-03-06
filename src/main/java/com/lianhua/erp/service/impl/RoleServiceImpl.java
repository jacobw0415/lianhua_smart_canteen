package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Permission;
import com.lianhua.erp.domain.Role;
import com.lianhua.erp.dto.user.RoleDto;
import com.lianhua.erp.repository.PermissionRepository;
import com.lianhua.erp.repository.RoleRepository;
import com.lianhua.erp.security.SecurityUtils;
import com.lianhua.erp.service.RoleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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

        // 僅超級管理員可修改 ROLE_SUPER_ADMIN / ROLE_ADMIN 的權限設定，避免一般管理員變更管理員角色定義
        if ("ROLE_SUPER_ADMIN".equals(role.getName()) || "ROLE_ADMIN".equals(role.getName())) {
            if (!SecurityUtils.hasAuthority("admin:manage")) {
                throw new AccessDeniedException("僅超級管理員可修改系統管理員或超級管理員角色的權限設定。");
            }
        }

        // 🌿 保護 ROLE_SUPER_ADMIN：必須保留 admin:manage
        if ("ROLE_SUPER_ADMIN".equals(role.getName())) {
            if (permissionNames.stream().noneMatch(p -> p != null && p.equals("admin:manage"))) {
                throw new IllegalStateException("不得移除 ROLE_SUPER_ADMIN 的 admin:manage 權限。");
            }
        }

        // 🌿 保護關鍵系統角色：避免透過 API 將 ROLE_ADMIN 的核心管理權限移除
        if ("ROLE_ADMIN".equals(role.getName())) {
            String[] required = new String[] { "user:view", "user:edit", "role:view", "role:edit" };
            for (String requiredPerm : required) {
                if (permissionNames.stream().noneMatch(p -> p != null && p.equals(requiredPerm))) {
                    throw new IllegalStateException("不得移除 ROLE_ADMIN 的核心權限：" + requiredPerm);
                }
            }
            // ROLE_ADMIN 不得被賦予 admin:manage（僅 SUPER_ADMIN 可管理其他管理員）
            if (permissionNames.stream().anyMatch(p -> p != null && p.equals("admin:manage"))) {
                throw new IllegalStateException("ROLE_ADMIN 不得被賦予 admin:manage，僅 ROLE_SUPER_ADMIN 可擁有此權限。");
            }
        }

        // 🌿 一般使用者角色：不得賦予使用者管理／角色管理權限
        if ("ROLE_USER".equals(role.getName())) {
            String[] forbidden = new String[] { "user:view", "user:edit", "role:view", "role:edit", "admin:manage" };
            for (String perm : forbidden) {
                if (permissionNames.stream().anyMatch(p -> p != null && p.equals(perm))) {
                    throw new IllegalStateException("ROLE_USER 不得被賦予管理權限：" + perm + "，僅限一般業務權限（如 order:view、purchase:view 等）。");
                }
            }
        }

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