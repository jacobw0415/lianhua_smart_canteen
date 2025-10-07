package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.Role;
import com.lianhua.erp.domin.User;
import com.lianhua.erp.dto.RoleDto;
import com.lianhua.erp.dto.user.UserDto;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {
    public UserDto toDto(User user) {
        if (user == null) return null;
        
        System.out.println("🧭 UserMapper converting user: " + user.getUsername());
        
        Set<RoleDto> roleDtos = Set.of(); // Default empty set
        if (user.getUserRoles() != null) {
            System.out.println("🔍 userRoles count: " + user.getUserRoles().size());
            roleDtos = user.getUserRoles().stream()
                    .map(userRole -> {
                        Role role = userRole.getRole();
                        System.out.println("➡ role: " + role.getId() + " - " + role.getName());
                        return new RoleDto(role.getId(), role.getName());
                    })
                    .collect(Collectors.toSet());
        } else {
            System.out.println("⚠️ user.getUserRoles() is null");
        }
        
        System.out.println("🧭 Mapped roles: " + roleDtos.size());
        
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .enabled(user.getEnabled())
                .roles(roleDtos)
                .build();
    }
}