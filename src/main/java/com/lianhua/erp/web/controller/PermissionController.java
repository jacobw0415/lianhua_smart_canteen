package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.ForbiddenResponse;
import com.lianhua.erp.dto.error.UnauthorizedResponse;
import com.lianhua.erp.dto.user.PermissionDto;
import com.lianhua.erp.service.PermissionService;
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

/**
 * 權限定義 API
 * 供管理員取得權限清單，用於角色權限矩陣 UI 或權限對照
 */
@RestController
@RequestMapping("/api/permissions")
@Tag(name = "角色與權限管理", description = "提供管理員維護 ERP 角色與權限映射功能")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "取得權限清單", description = "取得所有權限定義，可選依模組篩選 (module)，供角色權限矩陣勾選使用")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得列表"),
            @ApiResponse(responseCode = "401", description = "未授權（請重新登入）", content = @Content(schema = @Schema(implementation = UnauthorizedResponse.class))),
            @ApiResponse(responseCode = "403", description = "權限不足（需管理員權限）", content = @Content(schema = @Schema(implementation = ForbiddenResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('role:view')")
    public ResponseEntity<ApiResponseDto<List<PermissionDto>>> getAllPermissions(
            @RequestParam(required = false) String module) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDto.ok(permissionService.getAllPermissions(module)));
    }
}
