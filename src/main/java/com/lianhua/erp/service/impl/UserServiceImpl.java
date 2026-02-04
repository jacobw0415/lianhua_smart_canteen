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

    /** 取得單一使用者 - 🌿 引用 findByIdWithRoles 讓燈號亮起 */
    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        // 使用 JOIN FETCH 版本，效能更佳
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("找不到使用者帳號: " + username));
        return userMapper.toDto(user);
    }

    /** 管理員建立使用者 - 🌿 引用 existsByUsername & existsByEmail 讓燈號亮起 */
    @Override
    @Transactional
    public UserDto createUser(UserRequestDto dto) {
        // 1. 唯一性校驗 (防禦性編程)
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("帳號名稱已存在: " + dto.getUsername());
        }
        if (dto.getEmail() != null && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email 已經被使用: " + dto.getEmail());
        }

        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .employee_id(dto.getEmployeeId())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .roles(new HashSet<>())
                .build();

        if (dto.getRoleNames() != null && !dto.getRoleNames().isEmpty()) {
            for (String roleName : dto.getRoleNames()) {
                Role role = roleRepository.findByName(roleName.toUpperCase())
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));
                user.addRole(role);
            }
        }

        return userMapper.toDto(userRepository.save(user));
    }

    /** 使用者註冊 - 🌿 引用 existsByUsername 確保註冊安全 */
    @Override
    @Transactional
    public UserDto registerUser(UserRegisterDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("該帳號名稱已被註冊");
        }

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

        // 若修改 Email，需檢查是否與他人重複
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("新的 Email 已經被其他帳號使用");
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getEnabled() != null) user.setEnabled(dto.getEnabled());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getRoleNames() != null) {
            Set<Role> newRoles = dto.getRoleNames().stream()
                    .map(name -> roleRepository.findByName(name.toUpperCase())
                            .orElseThrow(() -> new EntityNotFoundException("Role not found: " + name)))
                    .collect(Collectors.toSet());
            user.setRoles(newRoles);
        }

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }
}