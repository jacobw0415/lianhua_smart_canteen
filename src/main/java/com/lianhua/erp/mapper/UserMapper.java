package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.User;
import com.lianhua.erp.dto.user.UserDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) return null;

        System.out.println("🧭 UserMapper converting user: " + user.getUsername());

        List<String> roleNames = List.of(); // 預設空清單

        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            System.out.println("🔍 userRoles count: " + user.getUserRoles().size());
            roleNames = user.getUserRoles().stream()
                    .map(userRole -> {
                        String roleName = userRole.getRole().getName();
                        System.out.println("➡ role: " + roleName);
                        return roleName;
                    })
                    .distinct() // 避免重複角色
                    .collect(Collectors.toList());
        } else {
            System.out.println("⚠️ user.getUserRoles() is null or empty");
        }

        System.out.println("🧭 Mapped role names: " + roleNames);

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .enabled(user.getEnabled())
                .roles(roleNames) // ✅ 改為 List<String>
                .build();
    }
}
