package com.lianhua.erp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全相關共用工具。
 * 提供從 Spring Security 取得當前登入使用者資訊的便捷方法，避免各處重複轉型與 NPE 風險。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 取得當前登入者的使用者 ID；若尚未登入或 Principal 型別不是 CustomUserDetails，則回傳 null。
     */
    public static Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getId();
        }
        return null;
    }

    /**
     * 取得當前登入者帳號；若尚未登入則回傳 null。
     */
    public static String getCurrentUsernameOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getName();
    }
}

