package com.lianhua.erp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

    /**
     * 判斷當前登入者是否擁有指定權限（含角色，如 ROLE_ADMIN、權限如 user:edit、admin:manage）。
     */
    public static boolean hasAuthority(String authority) {
        if (authority == null || authority.isBlank()) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> authority.equals(a));
    }

    /**
     * 判斷當前登入者是否擁有指定角色（會自動補上 ROLE_ 前綴若未提供）。
     */
    public static boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return hasAuthority(normalized);
    }
}

