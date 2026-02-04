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

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 從資料庫查找使用者，並根據加強版 EAGER 設定載入角色
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("找不到使用者: " + username));

        // 2. 將 Roles 與 Permissions 轉換為 Spring Security 的 GrantedAuthority
        // 包含角色 (需帶 ROLE_ 前綴) 與具體權限 (如 purchase:void)
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    // 加入角色本身
                    java.util.stream.Stream<String> roleName = java.util.stream.Stream.of(role.getName());
                    // 加入該角色擁有的所有具體權限
                    java.util.stream.Stream<String> permissions = role.getPermissions().stream()
                            .map(com.lianhua.erp.domain.Permission::getName);
                    return java.util.stream.Stream.concat(roleName, permissions);
                })
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 3. 回傳 Spring Security 內建的 User 物件
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}