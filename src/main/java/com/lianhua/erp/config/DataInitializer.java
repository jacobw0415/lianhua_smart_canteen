package com.lianhua.erp.config;

import com.lianhua.erp.domain.Permission;
import com.lianhua.erp.domain.Role;
import com.lianhua.erp.domain.User;
import com.lianhua.erp.repository.PermissionRepository;
import com.lianhua.erp.repository.RoleRepository;
import com.lianhua.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Transactional // 🌿 確保關聯儲存時處於同一事務
    CommandLineRunner initSystemData() {
        return args -> {
            log.info("🌿 開始初始化 Lianhua ERP v2.7 系統基礎數據...");

            // 1. 初始化顆粒度權限 (Permissions)
            // 這些權限將在未來配合 @PreAuthorize("hasAuthority('...')") 使用
            Permission pUserView = createPermissionIfNotFound("user:view", "查看使用者", "系統管理");
            Permission pUserEdit = createPermissionIfNotFound("user:edit", "編輯使用者", "系統管理");
            Permission pOrderView = createPermissionIfNotFound("order:view", "查看訂單", "訂單管理");
            Permission pOrderEdit = createPermissionIfNotFound("order:edit", "處理訂單", "訂單管理");

            // 2. 初始化角色 (Roles)
            // ROLE_ADMIN: 擁有系統所有權限
            Role adminRole = createRoleIfNotFound("ROLE_ADMIN", "系統管理員",
                    Set.of(pUserView, pUserEdit, pOrderView, pOrderEdit));

            // ROLE_USER: 僅具備基礎查看權限
            Role userRole = createRoleIfNotFound("ROLE_USER", "一般員工",
                    Set.of(pOrderView));

            // 3. 初始化管理員帳號 (Admin User)
            // 必須確保具備 ROLE_ADMIN，JwtUtils 才能在 Claim 加入正確的 roles
            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("系統管理員")
                        .email("admin@lianhua.com")
                        .enabled(true)
                        .roles(new HashSet<>()) // 初始化集合避免 NullPointerException
                        .build();

                admin.addRole(adminRole); // 建立多對多關聯
                userRepository.save(admin);
                log.info("✅ 初始管理員帳號 'admin' 建立完成 (預設密碼: admin123)");
            }

            log.info("🌿 系統數據初始化檢查結束。");
        };
    }

    private Permission createPermissionIfNotFound(String name, String description, String module) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    Permission p = Permission.builder()
                            .name(name)
                            .description(description)
                            .module(module)
                            .build();
                    log.info("   -> 建立權限: {}", name);
                    return permissionRepository.save(p);
                });
    }

    private Role createRoleIfNotFound(String name, String description, Set<Permission> permissions) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role r = Role.builder()
                            .name(name)
                            .description(description)
                            .permissions(permissions)
                            .build();
                    log.info("   -> 建立角色: {}", name);
                    return roleRepository.save(r);
                });
    }
}