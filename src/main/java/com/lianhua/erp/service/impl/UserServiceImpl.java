package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.Role;
import com.lianhua.erp.domin.User;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

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
        User user = userRepository.findByIdWithRoles(id);
        if (user == null)
            throw new EntityNotFoundException("User not found: " + id);
        System.out.println("✅ Loaded user with roles count: " + user.getRoles().size());
        return userMapper.toDto(user);
    }

    /** 管理員建立使用者（使用角色名稱） */
    @Override
    public UserDto createUser(UserRequestDto dto) {
        Set<Role> roles = new HashSet<>();

        if (dto.getRoleNames() != null && !dto.getRoleNames().isEmpty()) {
            roles = dto.getRoleNames().stream()
                    .map(name -> roleRepository.findByName(name)
                            .orElseThrow(() -> new EntityNotFoundException("Role not found: " + name)))
                    .collect(Collectors.toSet());
        }

        User user = User.builder()
                .username(dto.getUsername())
                .fullName(dto.getFullName())
                .password(passwordEncoder.encode(dto.getPassword()))
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .roles(roles)
                .build();

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
                .roles(Set.of(defaultRole))
                .build();

        return userMapper.toDto(userRepository.save(user));
    }

    /** 更新使用者（限管理員） */
    @Override
    public UserDto updateUser(Long id, UserRequestDto dto) {
        User user = userRepository.findByIdWithRoles(id);
        if (user == null)
            throw new EntityNotFoundException("User not found: " + id);

        user.setFullName(dto.getFullName());
        user.setEnabled(dto.getEnabled());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getRoleNames() != null && !dto.getRoleNames().isEmpty()) {
            Set<Role> roles = dto.getRoleNames().stream()
                    .map(name -> roleRepository.findByName(name)
                            .orElseThrow(() -> new EntityNotFoundException("Role not found: " + name)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        return userMapper.toDto(userRepository.save(user));
    }

    /** 刪除使用者 */
    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
