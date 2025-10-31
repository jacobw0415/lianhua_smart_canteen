package com.lianhua.erp.config;

import com.lianhua.erp.domain.Role;
import com.lianhua.erp.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            createRoleIfNotExists(roleRepository, "ADMIN");
            createRoleIfNotExists(roleRepository, "USER");
        };
    }

    private void createRoleIfNotExists(RoleRepository repo, String roleName) {
        if (repo.findByName(roleName).isEmpty()) {
            repo.save(Role.builder().name(roleName).build());
            System.out.printf("✅ Role '%s' initialized.%n", roleName);
        }
    }
}
