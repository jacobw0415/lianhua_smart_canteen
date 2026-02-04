package com.lianhua.erp.security;

import com.lianhua.erp.domain.User;
import com.lianhua.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 從資料庫查找使用者
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("找不到使用者: " + username));

        // 2. 將 Roles 與 Permissions 轉換為 Authorities
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    Stream<String> roleName = Stream.of(role.getName());
                    Stream<String> permissions = role.getPermissions().stream()
                            .map(com.lianhua.erp.domain.Permission::getName);
                    return Stream.concat(roleName, permissions);
                })
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 3. ✅ 關鍵修改：回傳自定義的 CustomUserDetails
        // 這樣 JwtUtils 才能正確提取 user.getId() (即 uid)
        return new CustomUserDetails(
                user.getId(),           // 🌿 傳入資料庫的 ID，對應 JWT 的 uid
                user.getUsername(),     // 帳號
                user.getPassword(),     // 加密後的密碼
                user.getEnabled(),      // 帳號啟用狀態
                authorities             // 權限清單
        );
    }
}