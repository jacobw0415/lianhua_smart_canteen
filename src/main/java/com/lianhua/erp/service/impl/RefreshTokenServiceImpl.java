package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.MfaPendingSession;
import com.lianhua.erp.domain.RefreshToken;
import com.lianhua.erp.domain.User;
import com.lianhua.erp.dto.user.JwtResponse;
import com.lianhua.erp.repository.MfaPendingSessionRepository;
import com.lianhua.erp.repository.RefreshTokenRepository;
import com.lianhua.erp.repository.UserRepository;
import com.lianhua.erp.security.CustomUserDetails;
import com.lianhua.erp.security.JwtUtils;
import com.lianhua.erp.service.MfaService;
import com.lianhua.erp.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MfaPendingSessionRepository mfaPendingSessionRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final MfaService mfaService;

    @Value("${lianhua.app.refreshTokenExpirationSeconds:604800}")
    private long refreshTokenExpirationSeconds;

    @Value("${lianhua.app.mfaPendingExpirationSeconds:300}")
    private long mfaPendingExpirationSeconds;

    private static String hashToken(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    @Transactional
    public String issueRefreshToken(Long userId) {
        String plain = UUID.randomUUID().toString();
        String tokenHash = hashToken(plain);
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenExpirationSeconds);

        RefreshToken rt = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(rt);
        return plain;
    }

    @Override
    @Transactional
    public JwtResponse refreshAccessToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        RefreshToken rt = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh Token 無效或已撤銷"));

        if (rt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh Token 已過期");
        }

        User user = userRepository.findByIdWithRoles(rt.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在"));

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    var roleName = List.<GrantedAuthority>of(new SimpleGrantedAuthority(role.getName()));
                    var perms = role.getPermissions().stream()
                            .map(p -> new SimpleGrantedAuthority(p.getName()))
                            .toList();
                    return java.util.stream.Stream.concat(roleName.stream(), perms.stream());
                })
                .distinct()
                .toList();

        CustomUserDetails details = new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getEnabled()),
                authorities
        );

        String accessToken = jwtUtils.generateJwtToken(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(details, null, authorities)
        );

        JwtResponse response = new JwtResponse();
        response.setToken(accessToken);
        response.setType("Bearer");
        response.setExpiresIn(jwtUtils.getJwtExpirationSeconds());
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRoles(authorities.stream().map(GrantedAuthority::getAuthority).toList());
        response.setRoleNames(authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && a.startsWith("ROLE_"))
                .toList());

        // 可選：Refresh Token 輪替（撤銷舊的、發新的）
        revokeRefreshToken(refreshToken);
        response.setRefreshToken(issueRefreshToken(user.getId()));

        return response;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash).ifPresent(rt -> {
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    @Override
    @Transactional
    public String createMfaPending(Long userId) {
        mfaPendingSessionRepository.deleteByUserId(userId);
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(mfaPendingExpirationSeconds);
        MfaPendingSession session = MfaPendingSession.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        mfaPendingSessionRepository.save(session);
        return token;
    }

    @Override
    @Transactional
    public JwtResponse verifyMfaAndIssueTokens(String pendingToken, String code) {
        MfaPendingSession session = mfaPendingSessionRepository.findByTokenAndExpiresAtAfter(pendingToken, Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("MFA 暫存無效或已過期"));

        User user = userRepository.findByIdWithRoles(session.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在"));

        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new IllegalStateException("該帳號尚未設定 MFA 密鑰");
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            throw new IllegalArgumentException("MFA 驗證碼錯誤");
        }

        mfaPendingSessionRepository.delete(session);

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    var roleName = List.<GrantedAuthority>of(new SimpleGrantedAuthority(role.getName()));
                    var perms = role.getPermissions().stream()
                            .map(p -> new SimpleGrantedAuthority(p.getName()))
                            .toList();
                    return java.util.stream.Stream.concat(roleName.stream(), perms.stream());
                })
                .distinct()
                .toList();

        CustomUserDetails details = new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getEnabled()),
                authorities
        );

        String accessToken = jwtUtils.generateJwtToken(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(details, null, authorities)
        );

        String newRefreshToken = issueRefreshToken(user.getId());

        JwtResponse response = new JwtResponse();
        response.setToken(accessToken);
        response.setRefreshToken(newRefreshToken);
        response.setType("Bearer");
        response.setExpiresIn(jwtUtils.getJwtExpirationSeconds());
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRoles(authorities.stream().map(GrantedAuthority::getAuthority).toList());
        response.setRoleNames(authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && a.startsWith("ROLE_"))
                .toList());

        return response;
    }
}
