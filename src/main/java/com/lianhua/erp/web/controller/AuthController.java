package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.auth.ForgotPasswordRequest;
import com.lianhua.erp.dto.auth.ResetPasswordRequest;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.user.JwtResponse;
import com.lianhua.erp.dto.user.UserRegisterDto;
import com.lianhua.erp.dto.user.UserDto;
import com.lianhua.erp.security.CustomUserDetails;
import com.lianhua.erp.security.JwtUtils;
import com.lianhua.erp.service.AuthService;
import com.lianhua.erp.service.LoginAttemptService;
import com.lianhua.erp.service.LoginLogService;
import com.lianhua.erp.service.PasswordResetService;
import com.lianhua.erp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.lianhua.erp.dto.user.LoginRequest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

/**
 * 認證控制中心
 * 負責處理登入、登出與公開註冊請求
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "認證管理", description = "登入與註冊相關 API (公開路徑)")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final LoginLogService loginLogService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils,
                          UserService userService,
                          AuthService authService,
                          PasswordResetService passwordResetService,
                          LoginLogService loginLogService,
                          LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.loginLogService = loginLogService;
        this.loginAttemptService = loginAttemptService;
    }

    // ============================================================
    // 🔑 登入認證 (Login)
    // ============================================================
    @Operation(summary = "使用者登入", description = "驗證帳密並回傳 JWT Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登入成功",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "帳號或密碼錯誤",
                    content = @Content(schema = @Schema(implementation = UnauthorizedResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping("/login")
    public ApiResponseDto<JwtResponse> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        // 基於 IP + 帳號的簡易登入頻率限制
        String clientIp = request.getRemoteAddr();
        String attemptKey = clientIp + "|" + loginRequest.getUsername();
        if (loginAttemptService.isBlocked(attemptKey)) {
            throw new IllegalStateException("登入嘗試過多，請稍後再試");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } catch (AuthenticationException ex) {
            // 登入失敗稽核記錄（不打斷原本的錯誤流程）
            loginLogService.logFailure(loginRequest.getUsername(), request);
            loginAttemptService.recordFailure(attemptKey);
            throw ex;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);

        // 這裡轉回你的 CustomUserDetails
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        Long userId = principal.getId();
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        // 僅角色代碼（ROLE_ 開頭），供前端角色選單/下拉使用，避免與權限混在一起出現多種選項
        List<String> roleNames = roles.stream()
                .filter(a -> a != null && a.startsWith("ROLE_"))
                .toList();

        // 更新該使用者的最後登入時間（last_login_at），供個人資料頁顯示
        userService.updateLastLoginAt(userId);
        // 成功則重置登入失敗計數
        loginAttemptService.reset(attemptKey);
        // 寫入登入成功稽核日誌
        loginLogService.logSuccess(userId, request);

        JwtResponse body = new JwtResponse();
        body.setId(userId);                 // ✅ 前端會存成 localStorage.userId
        body.setToken(jwt);
        body.setType("Bearer");
        body.setUsername(principal.getUsername());
        body.setRoles(roles);              // ✅ 完整 authorities（角色+權限），供權限判斷
        body.setRoleNames(roleNames);       // ✅ 僅角色，供「角色代碼」顯示/選項用

        return ApiResponseDto.ok(body);
    }

    // ============================================================
    // 📧 忘記密碼 (Forgot Password)
    // ============================================================
    @Operation(summary = "忘記密碼 - 發送重設郵件", description = "接收 Email 並發送帶有 Token 的重設連結")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "郵件發送成功"),
            @ApiResponse(responseCode = "404", description = "找不到該 Email 關聯的帳號",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PostMapping("/forgot-password") // 🌿 調用此處會讓 Service.processForgotPassword 亮燈
    public ApiResponseDto<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.processForgotPassword(request);
        return ApiResponseDto.ok("重設連結已發送至您的信箱，請於 15 分鐘內完成操作");
    }

    // ============================================================
    // 🔑 重設密碼 (Reset Password)
    // ============================================================
    @Operation(summary = "提交重設密碼", description = "驗證郵件中的 Token 並更新為新密碼")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "密碼重設成功"),
            @ApiResponse(responseCode = "400", description = "Token 無效或已過期",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class)))
    })
    @PostMapping("/reset-password") // 🌿 調用此處會讓 Service.resetPassword 亮燈
    public ApiResponseDto<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ApiResponseDto.ok("密碼已成功更新，請使用新密碼登入");
    }

    // ============================================================
    // 🚪 登出處理 (Logout) - 符合報告規格
    // ============================================================
    @Operation(summary = "使用者登出", description = "從 Authorization 讀取 Token 並使之失效。不論成功與否皆回傳 204。")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "已成功處理登出請求 (無內容回傳)"),
            @ApiResponse(responseCode = "500", description = "此端點已進行 try-catch 處理，不應回傳 500",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader); // 🌿 呼叫 Service 處理
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // ============================================================
    // 📝 公開註冊 (Register)
    // ============================================================
    @Operation(summary = "公開註冊", description = "一般使用者自行註冊，預設賦予 ROLE_USER 角色")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "註冊成功",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "參數格式錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "409", description = "使用者帳號已存在",
                    content = @Content(schema = @Schema(implementation = ConflictResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping("/register")
    public ApiResponseDto<UserDto> registerUser(@Valid @RequestBody UserRegisterDto registerDto) {
        return ApiResponseDto.ok(userService.registerUser(registerDto));
    }
}