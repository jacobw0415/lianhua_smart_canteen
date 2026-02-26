package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.user.*;
import com.lianhua.erp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 使用者管理 API
 * 負責提供管理員維護 ERP 帳號，以及一般使用者查詢個人資料
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "使用者管理", description = "提供管理員維護 ERP 帳號、權限角色與員工關聯之功能")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ============================================================
    // 👋 個人功能區 (不限角色，只要登入即可存取)
    // ============================================================

    @Operation(summary = "取得當前登入者個人資料", description = "供使用者查看自己的 Profile，解決前端個人資料載入問題")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得個人資料"),
            @ApiResponse(responseCode = "401", description = "未授權（請重新登入）", content = @Content(schema = @Schema(implementation = UnauthorizedResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<UserDto>> getCurrentUserProfile() {
        // 從 SecurityContext 中取得目前經過 JWT 認證的帳號名稱
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // 呼叫 userService 根據 username 查詢 (不依賴 URL 中的 ID)
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDto.ok(userService.getUserByUsername(currentUsername)));
    }

    // ============================================================
    // ⚙️ 管理員功能區 (需具備 ROLE_ADMIN 權限)
    // ============================================================

    @Operation(summary = "取得所有使用者", description = "取得所有帳號清單，包含其角色與基本資訊")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得列表"),
            @ApiResponse(responseCode = "403", description = "權限不足（需管理員權限）", content = @Content(schema = @Schema(implementation = ForbiddenResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('user:view')")
    public ResponseEntity<ApiResponseDto<List<UserDto>>> getAllUsers() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDto.ok(userService.getAllUsers()));
    }

    @Operation(summary = "取得指定使用者詳細資訊")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:view')")
    public ResponseEntity<ApiResponseDto<UserDto>> getUserById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDto.ok(userService.getUserById(id)));
    }

    @Operation(summary = "建立使用者", description = "由管理員手動建立帳號")
    @PostMapping
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<ApiResponseDto<UserDto>> createUser(@Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(userService.createUser(dto)));
    }

    @Operation(summary = "更新使用者資訊")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<ApiResponseDto<UserDto>> updateUser(
            @PathVariable Long id, @Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDto.ok(userService.updateUser(id, dto)));
    }

    @Operation(summary = "刪除使用者帳號")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}