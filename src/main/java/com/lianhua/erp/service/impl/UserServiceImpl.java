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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    
    /** 取得所有使用者 */
    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
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
        
        System.out.println("✅ Loaded user with roles count: " + user.getUserRoles().size());
        return userMapper.toDto(user);
    }
    
    /** 管理員建立使用者（使用角色名稱） */
    @Override
    public UserDto createUser(UserRequestDto dto) {
        User user = User.builder()
                .username(dto.getUsername())
                .fullName(dto.getFullName())
                .password(passwordEncoder.encode(dto.getPassword()))
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .build();
        
        // 建立 user 與 role 的關聯
        if (dto.getRoleNames() != null && !dto.getRoleNames().isEmpty()) {
            Set<UserRole> userRoles = dto.getRoleNames().stream()
                    .map(name -> {
                        Role role = roleRepository.findByName(name)
                                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + name));
                        return new UserRole(user, role);
                    })
                    .collect(Collectors.toSet());
            user.setUserRoles(userRoles);
        }
        
        return userMapper.toDto(userRepository.save(user));
    }
    
    /** 一般使用者註冊（自動給 USER 角色） */
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
        
        // 新增預設 USER 關聯
        UserRole ur = new UserRole(user, defaultRole);
        user.getUserRoles().add(ur);
        
        return userMapper.toDto(userRepository.save(user));
    }
    
    /** 更新使用者（限管理員） */
    @Override
    public UserDto updateUser(Long id, UserRequestDto dto) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        
        user.setFullName(dto.getFullName());
        user.setEnabled(dto.getEnabled());
        
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        
        // 更新角色
        if (dto.getRoleNames() != null && !dto.getRoleNames().isEmpty()) {
            Set<UserRole> updatedUserRoles = dto.getRoleNames().stream()
                    .map(name -> {
                        Role role = roleRepository.findByName(name)
                                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + name));
                        return new UserRole(user, role);
                    })
                    .collect(Collectors.toSet());
            
            user.getUserRoles().clear();
            user.getUserRoles().addAll(updatedUserRoles);
        }
        
        return userMapper.toDto(userRepository.save(user));
    }
    
    /** 刪除使用者 */
    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
