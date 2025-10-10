package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.User;
import com.lianhua.erp.dto.user.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.stream.Collectors;

/**
 *  User 實體 ↔ UserDto 轉換器
 * 防範 LazyInitializationException + NullPointer
 */
@Mapper(componentModel = "spring", imports = {Collectors.class})
public interface UserMapper {
    
    @Mapping(
            target = "roles",
            expression = "java(user.getUserRoles() == null ? java.util.List.of() :" +
                    " user.getUserRoles().stream()" +
                    ".filter(ur -> ur.getRole() != null && ur.getRole().getName() != null)" +
                    ".map(ur -> ur.getRole().getName())" +
                    ".distinct().collect(Collectors.toList()))"
    )
    UserDto toDto(User user);
}
