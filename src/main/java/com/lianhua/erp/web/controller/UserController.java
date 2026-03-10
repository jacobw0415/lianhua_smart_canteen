package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.user.*;
import com.lianhua.erp.security.SecurityUtils;
import com.lianhua.erp.service.UserService;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        @Operation(summary = "取得當前登入者個人資料", description = "供使用者查看自己的 Profile；回應含 id，供前端判斷是否為「編輯自己」")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "成功取得個人資料"),
                        @ApiResponse(responseCode = "401", description = "未授權（請重新登入）", content = @Content(schema = @Schema(implementation = UnauthorizedResponse.class)))
        })
        @GetMapping("/me")
        public ResponseEntity<ApiResponseDto<UserDto>> getCurrentUserProfile() {
                String currentUsername = SecurityUtils.getCurrentUsernameOrNull();
                if (currentUsername == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(ApiResponseDto.error(401, "請先登入"));
                }
                return ResponseEntity.status(HttpStatus.OK)
                                .body(ApiResponseDto.ok(userService.getUserByUsername(currentUsername)));
        }

        @Operation(summary = "本人修改密碼", description = "驗證目前密碼後更新為新密碼（§4.4）；與管理員重設他人密碼分離")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "密碼已更新"),
                        @ApiResponse(responseCode = "400", description = "目前密碼錯誤或新密碼不符合規則", content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
                        @ApiResponse(responseCode = "401", description = "未授權", content = @Content(schema = @Schema(implementation = UnauthorizedResponse.class)))
        })
        @PutMapping("/me/password")
        public ResponseEntity<ApiResponseDto<String>> changeOwnPassword(
                        @Valid @RequestBody ChangePasswordRequest request) {
                Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
                if (currentUserId == null || currentUserId <= 0) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(ApiResponseDto.error(401, "請先登入"));
                }
                userService.changePasswordForCurrentUser(currentUserId, request.getCurrentPassword(),
                                request.getNewPassword());
                return ResponseEntity.ok(ApiResponseDto.ok("密碼已更新"));
        }

        // ============================================================
        // ⚙️ 管理員功能區 (需具備 ROLE_ADMIN 權限)
        // ============================================================

        @Operation(summary = "取得所有使用者", description = "取得所有帳號清單，包含其角色與基本資訊（不分頁）")
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

        @Operation(summary = "搜尋使用者（支援分頁 + 模糊搜尋）", description = """
                        可依帳號、姓名、Email、啟用狀態搜尋使用者。
                        支援 page / size / sort，自動整合 React-Admin 分頁。

                        範例：
                        /api/users/search?page=0&size=10&sort=username,asc&username=admin&enabled=true
                        """)
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "搜尋成功"),
                        @ApiResponse(responseCode = "403", description = "權限不足（需管理員權限）", content = @Content(schema = @Schema(implementation = ForbiddenResponse.class)))
        })
        @PageableAsQueryParam
        @GetMapping("/search")
        @PreAuthorize("hasAuthority('user:view')")
        public ResponseEntity<ApiResponseDto<Page<UserDto>>> searchUsers(
                        @ParameterObject UserSearchRequest request,
                        @ParameterObject Pageable pageable) {
                Page<UserDto> page = userService.searchUsers(request, pageable);
                return ResponseEntity.ok(ApiResponseDto.ok(page));
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
                Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponseDto.created(userService.createUser(dto, currentUserId)));
        }

        @Operation(summary = "更新使用者資訊", description = "受 R1/R2 業務規則約束：自己不可改角色/啟用；不可移除最後一位管理員")
        @PutMapping("/{id}")
        @PreAuthorize("hasAuthority('user:edit')")
        public ResponseEntity<ApiResponseDto<UserDto>> updateUser(
                        @PathVariable Long id, @Valid @RequestBody UserRequestDto dto) {
                Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
                return ResponseEntity.status(HttpStatus.OK)
                                .body(ApiResponseDto.ok(userService.updateUser(id, dto, currentUserId)));
        }

        @Operation(summary = "刪除使用者帳號", description = "受 D1/D2 約束：不可刪除自己、不可刪除最後一位系統管理員")
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAuthority('user:edit')")
        public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
                Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
                userService.deleteUser(id, currentUserId);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        @Operation(summary = "強制登出指定使用者", description = "撤銷該使用者所有 Refresh Token 並立即讓既有 Access Token 失效。僅超級管理員可執行。")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "強制登出已執行"),
                        @ApiResponse(responseCode = "403", description = "權限不足（需超級管理員）", content = @Content(schema = @Schema(implementation = ForbiddenResponse.class)))
        })
        @PostMapping("/{id}/force_logout")
        @PreAuthorize("hasRole('SUPER_ADMIN')")
        public ResponseEntity<Void> forceLogoutUser(@PathVariable Long id) {
                Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
                userService.forceLogoutUser(id, currentUserId);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
}