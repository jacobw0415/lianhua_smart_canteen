package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.auth.*;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.user.JwtResponse;
import com.lianhua.erp.dto.user.UserRegisterDto;
import com.lianhua.erp.dto.user.UserDto;
import com.lianhua.erp.repository.UserRepository;
import com.lianhua.erp.security.CustomUserDetails;
import com.lianhua.erp.security.JwtUtils;
import com.lianhua.erp.security.SecurityUtils;
import com.lianhua.erp.service.AuthService;
import com.lianhua.erp.service.LoginAttemptService;
import com.lianhua.erp.service.LoginLogService;
import com.lianhua.erp.service.PasswordResetService;
import com.lianhua.erp.service.RefreshTokenService;
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
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils,
                          UserService userService,
                          AuthService authService,
                          PasswordResetService passwordResetService,
                          LoginLogService loginLogService,
                          LoginAttemptService loginAttemptService,
                          RefreshTokenService refreshTokenService,
                          UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.loginLogService = loginLogService;
        this.loginAttemptService = loginAttemptService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
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
    public ApiResponseDto<?> authenticateUser(
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

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        Long userId = principal.getId();

        // 更新最後登入時間、重置失敗計數、寫入登入成功稽核
        userService.updateLastLoginAt(userId);
        loginAttemptService.reset(attemptKey);
        loginLogService.logSuccess(userId, request);

        // 若已啟用 MFA：不發放 JWT，回傳 pendingToken 供第二階段驗證
        var user = userRepository.findById(userId).orElseThrow(() -> new IllegalStateException("User not found"));
        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            String pendingToken = refreshTokenService.createMfaPending(userId);
            MfaPendingResponse mfaPending = MfaPendingResponse.builder()
                    .mfaRequired(true)
                    .pendingToken(pendingToken)
                    .build();
            return ApiResponseDto.ok(mfaPending);
        }

        String jwt = jwtUtils.generateJwtToken(authentication);
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        List<String> roleNames = roles.stream()
                .filter(a -> a != null && a.startsWith("ROLE_"))
                .toList();

        String refreshToken = refreshTokenService.issueRefreshToken(userId);
        JwtResponse body = new JwtResponse();
        body.setId(userId);
        body.setToken(jwt);
        body.setRefreshToken(refreshToken);
        body.setExpiresIn(jwtUtils.getJwtExpirationSeconds());
        body.setType("Bearer");
        body.setUsername(principal.getUsername());
        body.setRoles(roles);
        body.setRoleNames(roleNames);

        return ApiResponseDto.ok(body);
    }

    // ============================================================
    // 🔄 Refresh Token 換發 Access Token
    // ============================================================
    @Operation(summary = "以 Refresh Token 換發新 Access Token", description = "傳入登入時取得的 refreshToken，取得新的 access token 與 refreshToken（輪替）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "換發成功", content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "400", description = "Refresh Token 無效或已過期", content = @Content(schema = @Schema(implementation = BadRequestResponse.class)))
    })
    @PostMapping("/refresh")
    public ApiResponseDto<JwtResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        JwtResponse body = refreshTokenService.refreshAccessToken(request.getRefreshToken());
        return ApiResponseDto.ok(body);
    }

    // ============================================================
    // 📱 MFA 第二階段驗證（登入後需輸入 6 碼）
    // ============================================================
    @Operation(summary = "MFA 驗證", description = "登入需 MFA 時，以 pendingToken + 6 碼換取 JWT；或啟用 MFA 時以 code 確認")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "驗證成功（登入流程回傳 JWT；啟用流程無 body）"),
            @ApiResponse(responseCode = "400", description = "驗證碼錯誤或暫存過期", content = @Content(schema = @Schema(implementation = BadRequestResponse.class)))
    })
    @PostMapping("/mfa/verify")
    public ApiResponseDto<?> mfaVerify(@Valid @RequestBody MfaVerifyRequest request) {
        if (request.getPendingToken() != null && !request.getPendingToken().isBlank()) {
            JwtResponse body = refreshTokenService.verifyMfaAndIssueTokens(request.getPendingToken(), request.getCode());
            return ApiResponseDto.ok(body);
        }
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        if (userId == null) {
            throw new IllegalStateException("請先登入後再確認 MFA 驗證碼");
        }
        authService.mfaConfirmEnable(userId, request.getCode());
        return ApiResponseDto.ok("MFA 已啟用");
    }

    // ============================================================
    // 📱 MFA 綁定設定（需已登入）
    // ============================================================
    @Operation(summary = "取得 MFA 綁定設定", description = "取得 TOTP 密鑰與 otpauth URL，可轉成 QR Code 供 Google Authenticator 掃描；呼叫後需再以 /mfa/verify 提交一次驗證碼才算啟用")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = MfaSetupResponse.class))),
            @ApiResponse(responseCode = "401", description = "未登入", content = @Content(schema = @Schema(implementation = UnauthorizedResponse.class)))
    })
    @PostMapping("/mfa/setup")
    public ApiResponseDto<MfaSetupResponse> mfaSetup() {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        if (userId == null) {
            throw new IllegalStateException("請先登入");
        }
        MfaSetupResponse body = authService.mfaSetup(userId);
        return ApiResponseDto.ok(body);
    }

    @Operation(summary = "關閉 MFA", description = "已登入使用者關閉自己的雙因素認證；須提供當前 TOTP 驗證碼以確認身分，通過後清除密鑰並設為未啟用")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "MFA 已關閉"),
            @ApiResponse(responseCode = "400", description = "驗證碼錯誤或格式不符", content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "401", description = "未登入", content = @Content(schema = @Schema(implementation = UnauthorizedResponse.class)))
    })
    @PostMapping("/mfa/disable")
    public ApiResponseDto<String> mfaDisable(@Valid @RequestBody MfaDisableRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        if (userId == null) {
            throw new IllegalStateException("請先登入");
        }
        authService.mfaDisable(userId, request.getCode());
        return ApiResponseDto.ok("MFA 已關閉");
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
    public ApiResponseDto<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        // 基於 IP + Email 的頻率限制，防止忘記密碼端點被濫用
        String clientIp = httpRequest.getRemoteAddr();
        String attemptKey = "FORGOT|" + clientIp + "|" + request.getEmail();
        if (loginAttemptService.isBlocked(attemptKey)) {
            throw new IllegalStateException("忘記密碼請求過於頻繁，請稍後再試");
        }

        try {
            passwordResetService.processForgotPassword(request);
            // 成功則重置計數
            loginAttemptService.reset(attemptKey);
        } catch (RuntimeException ex) {
            // 包含找不到 email 等情況，一律計入失敗次數，避免被用來撞帳號
            loginAttemptService.recordFailure(attemptKey);
            throw ex;
        }

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
    public ApiResponseDto<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        // 基於 IP + Token 的頻率限制，避免重設端點被暴力嘗試
        String clientIp = httpRequest.getRemoteAddr();
        String attemptKey = "RESET|" + clientIp + "|" + request.getToken();
        if (loginAttemptService.isBlocked(attemptKey)) {
            throw new IllegalStateException("重設密碼請求過於頻繁，請稍後再試");
        }

        try {
            passwordResetService.resetPassword(request);
            loginAttemptService.reset(attemptKey);
        } catch (RuntimeException ex) {
            loginAttemptService.recordFailure(attemptKey);
            throw ex;
        }

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