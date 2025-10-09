package com.lianhua.erp.mapper;

import com.lianhua.erp.domin.User;
import com.lianhua.erp.dto.user.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// ✅ 關鍵：手動 import Collectors
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = {Collectors.class})
public interface UserMapper {

    @Mapping(
            target = "roles",
            expression = "java(user.getUserRoles() == null || user.getUserRoles().isEmpty() ? java.util.List.of() : user.getUserRoles().stream().map(ur -> ur.getRole().getName()).distinct().collect(Collectors.toList()))"
    )
    UserDto toDto(User user);
}
