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
 * 包含：註冊、查詢、建立、更新、刪除。
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "使用者管理", description = "使用者與角色管理 API")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ============================================================
    // 🟢 一般使用者註冊（自動套用 USER 角色）
    // ============================================================
    @Operation(summary = "使用者註冊（自動套用 USER 角色）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "註冊成功",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<UserDto>> registerUser(@Valid @RequestBody UserRegisterDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(userService.registerUser(dto)));
    }

    // ============================================================
    // ⚙️ 管理員功能區
    // ============================================================

    @Operation(summary = "取得所有使用者（含角色）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得所有使用者",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "403", description = "無權限",
                    content = @Content(schema = @Schema(implementation = ForbiddenResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<List<UserDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponseDto.ok(userService.getAllUsers()));
    }

    @Operation(summary = "取得指定使用者（含角色）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得使用者資訊",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "找不到使用者",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<UserDto>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.ok(userService.getUserById(id)));
    }

    @Operation(summary = "建立使用者（可指定角色）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功建立使用者",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409", description = "使用者已存在",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<UserDto>> createUser(@RequestBody UserRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created(userService.createUser(dto)));
    }

    @Operation(summary = "更新使用者資訊（可修改角色）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功更新使用者",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "更新參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到使用者",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<UserDto>> updateUser(
            @PathVariable Long id, @RequestBody UserRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.ok(userService.updateUser(id, dto)));
    }

    @Operation(summary = "刪除使用者（限管理員）")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "成功刪除使用者",
                    content = @Content(schema = @Schema(implementation = NoContentResponse.class))),
            @ApiResponse(responseCode = "404", description = "找不到使用者",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponseDto.deleted());
    }
}
