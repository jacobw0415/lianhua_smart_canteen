package com.lianhua.erp.service;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {

    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("密碼不可為空白");
        }

        String password = rawPassword.trim();

        if (password.length() < 8) {
            throw new IllegalArgumentException("密碼長度至少需 8 碼");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;

        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                hasUpper = true;
            } else if (Character.isLowerCase(ch)) {
                hasLower = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            }
            if (hasUpper && hasLower && hasDigit) {
                break;
            }
        }

        if (!(hasUpper && hasLower && hasDigit)) {
            throw new IllegalArgumentException("密碼需同時包含大寫字母、小寫字母與數字");
        }
    }
}

