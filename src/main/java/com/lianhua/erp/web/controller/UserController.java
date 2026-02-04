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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 使用者管理 API
 * 包含：查詢、建立、更新、刪除（註冊邏輯建議移至 AuthController）。
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
    // ⚙️ 管理員功能區 (RBAC 保護)
    // ============================================================

    @Operation(summary = "取得所有使用者（含角色與基本資訊）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得所有使用者",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "403", description = "無權限存取（需具備 ROLE_ADMIN）",
                    content = @Content(schema = @Schema(implementation = ForbiddenResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<List<UserDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponseDto.ok(userService.getAllUsers()));
    }

    @Operation(summary = "取得指定使用者詳細資訊")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得使用者資訊",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到該使用者 ID",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<UserDto>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(userService.getUserById(id)));
    }

    @Operation(summary = "建立使用者（支援角色設定與員工關聯）")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "成功建立使用者",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "參數錯誤（如 Email 格式不正確）",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409", description = "帳號或電子郵件已存在",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<UserDto>> createUser(@Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(userService.createUser(dto)));
    }

    @Operation(summary = "更新使用者資訊（含權限變更與帳號啟停）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功更新使用者",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "更新參數格式錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到該使用者 ID",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<UserDto>> updateUser(
            @PathVariable Long id, @Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(userService.updateUser(id, dto)));
    }

    @Operation(summary = "刪除使用者帳號")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "成功刪除使用者"),
            @ApiResponse(responseCode = "403", description = "無權限刪除",
                    content = @Content(schema = @Schema(implementation = ForbiddenResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到該使用者 ID",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}