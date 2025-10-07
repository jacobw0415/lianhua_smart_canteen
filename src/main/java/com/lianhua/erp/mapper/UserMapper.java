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
        Set<RoleDto> roleDtos = user.getRoles().stream()
                .map(role -> new RoleDto(role.getId(), role.getName()))
                .collect(Collectors.toSet());
        
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .enabled(user.getEnabled())
                .roles(roleDtos)
                .build();
    }
}
