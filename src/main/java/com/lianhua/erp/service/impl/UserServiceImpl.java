package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Role;
import com.lianhua.erp.domain.User;
import com.lianhua.erp.domain.UserRole;
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
    @Transactional
    public UserDto createUser(UserRequestDto dto) {
        // 1️⃣ 建立使用者基本資料
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .build();
        
        // 2️⃣ 先保存以產生 user.id
        user = userRepository.saveAndFlush(user);
        
        // 3️⃣ 若有指定角色，安全地建立 user_roles 關聯
        if (dto.getRoleNames() != null && !dto.getRoleNames().isEmpty()) {
            for (String roleName : dto.getRoleNames()) {
                Role role = roleRepository.findByName(roleName.toUpperCase())
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));
                UserRole userRole = new UserRole(user, role);
                user.addRole(userRole);
            }
        }
        
        // 4️⃣ 再保存一次以建立中介表
        user = userRepository.saveAndFlush(user);
        
        // 5️⃣ 預載入角色以避免懶加載問題
        user.getUserRoles().forEach(ur -> ur.getRole().getName());
        
        return userMapper.toDto(user);
    }
    
    /** 使用者註冊（自動給 USER 角色） */
    @Override
    @Transactional
    public UserDto registerUser(UserRegisterDto dto) {
        // 1️⃣ 確認角色存在
        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new EntityNotFoundException("Default role USER not found"));
        
        // 2️⃣ 建立 User
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .enabled(true)
                .build();
        
        // 3️⃣ 先保存 User 取得 ID（避免 TransientPropertyValueException）
        user = userRepository.saveAndFlush(user);
        
        // 4️⃣ 維護雙向關聯
        UserRole userRole = new UserRole(user, defaultRole);
        user.addRole(userRole);
        
        // 5️⃣ 再次保存（CascadeType.ALL 會自動建立 user_roles）
        user = userRepository.saveAndFlush(user);
        
        // 6️⃣ 預載入角色避免 LazyInitializationException
        user.getUserRoles().forEach(ur -> ur.getRole().getName());
        
        return userMapper.toDto(user);
    }
    
    /** 更新使用者資訊（限管理員） */
    @Override
    public UserDto updateUser(Long id, UserRequestDto dto) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            user.setUsername(dto.getUsername());
        }
        
        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }
        
        if (dto.getEnabled() != null) {
            user.setEnabled(dto.getEnabled());
        }
        
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        
        if (dto.getRoleNames() != null && !dto.getRoleNames().isEmpty()) {
            updateUserRoles(user, dto.getRoleNames());
        }
        
        userRepository.saveAndFlush(user);
        user.getUserRoles().forEach(ur -> ur.getRole().getName()); // ✅ 預先載入角色
        
        return userMapper.toDto(user);
    }
    
    /** 安全角色更新邏輯 */
    private void updateUserRoles(User user, Set<String> roleNames) {
        if (roleNames == null) return;
        
        Set<String> newRoleNames = roleNames.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        
        Set<String> currentRoleNames = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName().toUpperCase())
                .collect(Collectors.toSet());
        
        user.getUserRoles().removeIf(ur ->
                !newRoleNames.contains(ur.getRole().getName().toUpperCase())
        );
        
        for (String roleName : newRoleNames) {
            if (!currentRoleNames.contains(roleName)) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));
                user.addRole(new UserRole(user, role));
            }
        }
    }
    
    /** 刪除使用者 */
    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
