package com.lianhua.erp.service.impl;

import com.lianhua.erp.domin.User;
import com.lianhua.erp.domin.Role;
import com.lianhua.erp.dto.user.UserDto;
import com.lianhua.erp.dto.user.UserRequestDto;
import com.lianhua.erp.mapper.UserMapper;
import com.lianhua.erp.repository.UserRepository;
import com.lianhua.erp.repository.RoleRepository;
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
    
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }
    
    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return userMapper.toDto(user);
    }
    
    @Override
    public UserDto createUser(UserRequestDto dto) {
        Set<Role> roles = new HashSet<>();
        if (dto.getRoleIds() != null) {
            roles = dto.getRoleIds().stream()
                    .map(rid -> roleRepository.findById(rid)
                            .orElseThrow(() -> new EntityNotFoundException("Role not found: " + rid)))
                    .collect(Collectors.toSet());
        }
        
        User user = User.builder()
                .username(dto.getUsername())
                .fullName(dto.getFullName())
                .password(passwordEncoder.encode(dto.getPassword()))
                .enabled(dto.getEnabled())
                .roles(roles)
                .build();
        
        return userMapper.toDto(userRepository.save(user));
    }
    
    @Override
    public UserDto updateUser(Long id, UserRequestDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        
        user.setFullName(dto.getFullName());
        user.setEnabled(dto.getEnabled());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        
        if (dto.getRoleIds() != null) {
            Set<Role> roles = dto.getRoleIds().stream()
                    .map(rid -> roleRepository.findById(rid)
                            .orElseThrow(() -> new EntityNotFoundException("Role not found: " + rid)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }
        
        return userMapper.toDto(userRepository.save(user));
    }
    
    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
