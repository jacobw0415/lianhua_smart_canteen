package com.lianhua.erp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "登入成功回應")
public class JwtResponse {
    @Schema(description = "JWT 存取令牌")
    private String token;

    @Schema(description = "令牌類型", example = "Bearer")
    private String type = "Bearer";

    @Schema(description = "使用者帳號", example = "admin")
    private String username;

    // Added fields to match controller usage
    private Long id;
    private List<String> roles;

    public JwtResponse(String accessToken, String username) {
        this.token = accessToken;
        this.username = username;
    }
}