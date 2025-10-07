package com.lianhua.erp.mapper;

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

        Set<RoleDto> roleDtos = Set.of(); // 預設空集合
        if (user.getRoles() != null) {
            System.out.println("🔍 roles count: " + user.getRoles().size());
            user.getRoles().forEach(r -> System.out.println("➡ role: " + r.getId() + " - " + r.getName()));
            roleDtos = user.getRoles().stream()
                    .map(role -> new RoleDto(role.getId(), role.getName()))
                    .collect(Collectors.toSet());
        } else {
            System.out.println("⚠️ user.getRoles() is null");
        }

        System.out.println("🧭 Mapping roles: " + roleDtos.size());

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .enabled(user.getEnabled())
                .roles(roleDtos)
                .build();
    }
}
