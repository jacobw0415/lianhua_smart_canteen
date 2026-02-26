package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.Permission;
import com.lianhua.erp.dto.user.PermissionDto;
import com.lianhua.erp.repository.PermissionRepository;
import com.lianhua.erp.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getAllPermissions(String module) {
        List<Permission> list = StringUtils.hasText(module)
                ? permissionRepository.findByModule(module.trim())
                : permissionRepository.findAll();
        return list.stream()
                .sorted(Comparator.comparing(Permission::getModule, Comparator.nullsLast(String::compareTo))
                        .thenComparing(Permission::getName))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private PermissionDto toDto(Permission p) {
        return PermissionDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .module(p.getModule())
                .build();
    }
}
