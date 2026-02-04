package com.lianhua.erp.mapper;

import com.lianhua.erp.domain.Role;
import com.lianhua.erp.domain.User;
import com.lianhua.erp.dto.user.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User 實體 ↔ UserDto 轉換器
 * 配合 @ManyToMany 架構重構
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStrings")
    @Mapping(target = "employeeId", source = "employee_id") // 對應 Entity 欄位名稱
    UserDto toDto(User user);

    /**
     * 將 Role 實體集合轉換為字串列表 (供前端 React-Admin 使用)
     */
    @Named("mapRolesToStrings")
    default List<String> mapRolesToStrings(Collection<Role> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }
}