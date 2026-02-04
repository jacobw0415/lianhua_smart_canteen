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
    @Transactional
    CommandLineRunner initSystemData() {
        return args -> {
            log.info("🌿 開始初始化 Lianhua ERP v2.7 系統基礎數據...");

            // 1. 初始化具體權限 (Permissions)
            // 這是 ERP 顆粒度控制的核心
            Permission pView = createPermissionIfNotFound("purchase:view", "查看採購單", "進貨");
            Permission pVoid = createPermissionIfNotFound("purchase:void", "作廢採購單", "進貨");
            Permission sView = createPermissionIfNotFound("sale:view", "查看銷售", "銷售");

            // 2. 初始化角色並綁定權限 (Roles)
            // 注意：Spring Security 慣例建議加上 ROLE_ 前綴
            Role adminRole = createRoleIfNotFound("ROLE_ADMIN", "系統管理員", Set.of(pView, pVoid, sView));
            Role userRole = createRoleIfNotFound("ROLE_USER", "一般員工", Set.of(pView, sView));

            // 3. 初始化預設管理員帳號 (Admin User)
            // 確保系統啟動後有第一個可以登入的帳號
            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123")) // 請務必於登入後修改
                        .fullName("系統管理員")
                        .email("admin@lianhua.com") // 對應加強版 SQL 欄位
                        .enabled(true)
                        .build();

                admin.addRole(adminRole); // 建立 User 與 Role 的多對多關聯
                userRepository.save(admin);
                log.info("✅ 初始管理員帳號 'admin' 建立完成 (密碼: admin123)");
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