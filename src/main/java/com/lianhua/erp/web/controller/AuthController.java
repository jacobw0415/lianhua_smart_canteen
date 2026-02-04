package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import com.lianhua.erp.dto.user.UserRegisterDto;
import com.lianhua.erp.dto.user.UserDto;
import com.lianhua.erp.security.JwtUtils;
import com.lianhua.erp.service.AuthService;
import com.lianhua.erp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 認證控制中心
 * 負責處理登入、登出與公開註冊請求
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "01. 認證管理", description = "登入與註冊相關 API (公開路徑)")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final AuthService authService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils,
                          UserService userService, AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.authService = authService;
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
    public ApiResponseDto<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // 1. 使用 AuthenticationManager 進行帳密驗證
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 2. 驗證成功後，將認證資訊存入 SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. 產生 JWT Token
        String jwt = jwtUtils.generateJwtToken(authentication.getName());

        // 4. 回傳 Token 資訊
        return ApiResponseDto.ok(new JwtResponse(jwt, authentication.getName()));
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

    // ============================================================
    // 內部 DTO 類別
    // ============================================================

    @Data
    @Schema(description = "登入請求")
    public static class LoginRequest {
        @Schema(example = "admin")
        private String username;
        @Schema(example = "admin123")
        private String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "登入成功回應")
    public static class JwtResponse {
        @Schema(description = "JWT 存取令牌")
        private String token;

        @Schema(description = "令牌類型", example = "Bearer")
        private String type = "Bearer";

        @Schema(description = "使用者帳號", example = "admin")
        private String username;

        public JwtResponse(String accessToken, String username) {
            this.token = accessToken;
            this.username = username;
        }
    }
}