package com.lianhua.erp.service;

import com.lianhua.erp.domain.LoginLog;
import com.lianhua.erp.domain.User;
import com.lianhua.erp.repository.LoginLogRepository;
import com.lianhua.erp.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;
    private final UserRepository userRepository;

    public void logSuccess(Long userId, HttpServletRequest request) {
        User user = null;
        if (userId != null) {
            Optional<User> optionalUser = userRepository.findById(userId);
            user = optionalUser.orElse(null);
        }

        LoginLog log = LoginLog.builder()
                .user(user)
                .loginIp(extractClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .status(LoginLog.LoginStatus.SUCCESS)
                .build();

        loginLogRepository.save(log);
    }

    public void logFailure(String usernameOrEmail, HttpServletRequest request) {
        // 失敗時可能無法正確取得 User，僅記載 IP 與 User-Agent 供稽核與防護使用
        LoginLog log = LoginLog.builder()
                .user(null)
                .loginIp(extractClientIp(request))
                .userAgent((request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "") + " | account=" + (usernameOrEmail != null ? usernameOrEmail : ""))
                .status(LoginLog.LoginStatus.FAILED)
                .build();

        loginLogRepository.save(log);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // 多重代理時，取第一個 IP
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

