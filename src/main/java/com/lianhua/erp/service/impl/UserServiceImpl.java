package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Role;
import com.lianhua.erp.domain.User;
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

import java.util.HashSet;
import java.util.List;
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
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    /** 取得單一使用者 */
    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        // 註：User Entity 已設定 Roles 為 EAGER，此處直接 findById 即可
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return userMapper.toDto(user);
    }

    /** 管理員建立使用者（支援加強版欄位 email, employeeId） */
    @Override
    @Transactional
    public UserDto createUser(UserRequestDto dto) {
        // 1️⃣ 建立使用者基本資料 (包含新欄位)
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .email(dto.getEmail())           // 🌿 加強版新欄位
                .employee_id(dto.getEmployeeId()) // 🌿 加強版新欄位
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .roles(new HashSet<>())
                .build();

        // 2️⃣ 若有指定角色，直接從 Repository 取得 Role 放入 Set
        if (dto.getRoleNames() != null && !dto.getRoleNames().isEmpty()) {
            for (String roleName : dto.getRoleNames()) {
                Role role = roleRepository.findByName(roleName.toUpperCase())
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));
                user.addRole(role); // 簡化後的輔助方法
            }
        }

        // 3️⃣ 保存（JPA 會自動維護 user_roles 中間表）
        return userMapper.toDto(userRepository.save(user));
    }

    /** 使用者註冊（自動給予 USER 角色） */
    @Override
    @Transactional
    public UserDto registerUser(UserRegisterDto dto) {
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new EntityNotFoundException("Default role ROLE_USER not found"));

        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .enabled(true)
                .roles(new HashSet<>())
                .build();

        user.addRole(defaultRole);

        return userMapper.toDto(userRepository.save(user));
    }

    /** 更新使用者資訊 */
    @Override
    @Transactional
    public UserDto updateUser(Long id, UserRequestDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        // 更新基本資訊
        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getEnabled() != null) user.setEnabled(dto.getEnabled());

        // 更新密碼
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // 🌿 重構後：簡單的角色更新邏輯
        if (dto.getRoleNames() != null) {
            Set<Role> newRoles = dto.getRoleNames().stream()
                    .map(name -> roleRepository.findByName(name.toUpperCase())
                            .orElseThrow(() -> new EntityNotFoundException("Role not found: " + name)))
                    .collect(Collectors.toSet());

            user.setRoles(newRoles); // 直接替換即可，JPA 會自動處理刪除與新增
        }

        return userMapper.toDto(userRepository.save(user));
    }

    /** 刪除使用者 */
    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }
}