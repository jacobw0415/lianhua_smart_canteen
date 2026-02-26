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

    /** 角色＋權限合併清單（與 JWT 內 authorities 一致），供權限判斷用 */
    private List<String> roles;

    /** 僅角色代碼（ROLE_ 開頭），供前端「角色」顯示或選單用，避免與權限混為一談導致出現 ADMIN/USER/ROLE_ADMIN/ROLE_USER 多種選項 */
    private List<String> roleNames;

    public JwtResponse(String accessToken, String username) {
        this.token = accessToken;
        this.username = username;
    }
}