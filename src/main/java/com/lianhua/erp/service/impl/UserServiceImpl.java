package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.Role;
import com.lianhua.erp.domin.User;
import com.lianhua.erp.domin.UserRole;
import com.lianhua.erp.dto.user.UserDto;
import com.lianhua.erp.dto.user.UserRegisterDto;
import com.lianhua.erp.dto.user.UserRequestDto;
import com.lianhua.erp.mapper.UserMapper;
import com.lianhua.erp.repository.RoleRepository;
import com.lianhua.erp.repository.UserRepository;
import com.lianhua.erp.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    /** 取得所有使用者 */
    @Override
    @Transactional(readOnly = true)
    public java.util.List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    /** 取得單一使用者（含角色） */
    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return userMapper.toDto(user);
    }

    /** 管理員建立使用者（可指定角色） */
    @Override
    public UserDto createUser(UserRequestDto dto) {
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .build();

        if (dto.getRoleNames() != null && !dto.getRoleNames().isEmpty()) {
            updateUserRoles(user, dto.getRoleNames());
        }

        return userMapper.toDto(userRepository.save(user));
    }

    /** 使用者註冊（自動給 USER 角色） */
    @Override
    public UserDto registerUser(UserRegisterDto dto) {
        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new EntityNotFoundException("Default role USER not found"));

        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .enabled(true)
                .build();

        user.getUserRoles().add(new UserRole(user, defaultRole));
        return userMapper.toDto(userRepository.save(user));
    }

    /** 更新使用者資訊（限管理員） */
    @Override
    public UserDto updateUser(Long id, UserRequestDto dto) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        //  安全更新 username（避免覆蓋成 null 或空白）
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            user.setUsername(dto.getUsername());
        }

        //  更新全名（若有傳入）
        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }

        //  更新帳號啟用狀態
        if (dto.getEnabled() != null) {
            user.setEnabled(dto.getEnabled());
        }

        //  更新密碼（若非空）
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        //  改善：確保角色更新安全進行
        if (dto.getRoleNames() != null && !dto.getRoleNames().isEmpty()) {
            updateUserRoles(user, dto.getRoleNames());
        }

        //  儲存變更並回傳
        return userMapper.toDto(userRepository.save(user));
    }

    /** 專責角色更新邏輯（安全版） */
    private void updateUserRoles(User user, Set<String> roleNames) {
        if (roleNames == null) return;

        // 1️ 統一角色名稱大小寫，避免 "admin" vs "ADMIN"
        Set<String> newRoleNames = roleNames.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        // 2️ 找出目前使用者的角色名稱集合
        Set<String> currentRoleNames = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName().toUpperCase())
                .collect(Collectors.toSet());

        // 3️ 移除不再存在於新集合的角色
        user.getUserRoles().removeIf(ur ->
                !newRoleNames.contains(ur.getRole().getName().toUpperCase())
        );

        // 4⃣ 新增新集合中尚未存在的角色
        for (String roleName : newRoleNames) {
            if (!currentRoleNames.contains(roleName)) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));
                user.getUserRoles().add(new UserRole(user, role));
            }
        }
    }

    /** 刪除使用者 */
    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
