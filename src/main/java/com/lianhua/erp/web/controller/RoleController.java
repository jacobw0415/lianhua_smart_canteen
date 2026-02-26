package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.ForbiddenResponse;
import com.lianhua.erp.dto.error.NotFoundResponse;
import com.lianhua.erp.dto.error.UnauthorizedResponse;
import com.lianhua.erp.dto.user.RoleDto;
import com.lianhua.erp.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 角色與權限管理 API
 * 負責維護系統 RBAC 規則，對接 SQL Schema 中的 roles 與 permissions 表
 */
@RestController
@RequestMapping("/api/roles")
@Tag(name = "角色與權限管理", description = "提供管理員維護 ERP 角色與權限映射功能")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "取得所有角色清單", description = "供管理員維護或在指派使用者角色時選擇使用")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得列表"),
            @ApiResponse(responseCode = "401", description = "未授權（請重新登入）", content = @Content(schema = @Schema(implementation = UnauthorizedResponse.class))),
            @ApiResponse(responseCode = "403", description = "權限不足（需管理員權限）", content = @Content(schema = @Schema(implementation = ForbiddenResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('role:view')")
    public ResponseEntity<ApiResponseDto<List<RoleDto>>> getAllRoles() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDto.ok(roleService.getAllRoles()));
    }

    @Operation(summary = "取得單一角色詳情", description = "包含該角色擁有的所有細粒度權限 (Permissions)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得角色詳情"),
            @ApiResponse(responseCode = "404", description = "找不到該角色 ID", content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:view')")
    public ResponseEntity<ApiResponseDto<RoleDto>> getRoleById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDto.ok(roleService.getRoleById(id)));
    }

    @Operation(summary = "更新角色權限映射", description = "在 UI 權限矩陣勾選後，動態更新角色與權限的關聯 (role_permissions)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "權限更新成功"),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
            @ApiResponse(responseCode = "404", description = "角色或權限 ID 不存在", content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:edit')")
    public ResponseEntity<ApiResponseDto<RoleDto>> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody Set<String> permissionNames) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDto.ok(roleService.updateRolePermissions(id, permissionNames)));
    }
}