package com.lianhua.erp.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登入請求")
public class LoginRequest {
    @Schema(example = "admin")
    private String username;

    @Schema(example = "admin123")
    private String password;
}
