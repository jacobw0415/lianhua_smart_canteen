package com.lianhua.erp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lianhua.erp.domain.Role;
import com.lianhua.erp.domain.User;
import com.lianhua.erp.domain.UserAuditLog;
import com.lianhua.erp.dto.user.UserDto;
import com.lianhua.erp.dto.user.UserRegisterDto;
import com.lianhua.erp.dto.user.UserRequestDto;
import com.lianhua.erp.dto.user.UserSearchRequest;
import com.lianhua.erp.mapper.UserMapper;
import com.lianhua.erp.repository.RoleRepository;
import com.lianhua.erp.repository.UserAuditLogRepository;
import com.lianhua.erp.repository.UserRepository;
import com.lianhua.erp.security.SecurityUtils;
import com.lianhua.erp.service.RefreshTokenService;
import com.lianhua.erp.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserAuditLogRepository userAuditLogRepository;
    private final ObjectMapper objectMapper;
    private final com.lianhua.erp.service.PasswordPolicyValidator passwordPolicyValidator;
    private final RefreshTokenService refreshTokenService;
    private final com.lianhua.erp.security.SseSessionService sseSessionService;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           UserMapper userMapper,
                           UserAuditLogRepository userAuditLogRepository,
                           ObjectMapper objectMapper,
                           com.lianhua.erp.service.PasswordPolicyValidator passwordPolicyValidator,
                           RefreshTokenService refreshTokenService,
                           com.lianhua.erp.security.SseSessionService sseSessionService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userAuditLogRepository = userAuditLogRepository;
        this.objectMapper = objectMapper;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.refreshTokenService = refreshTokenService;
        this.sseSessionService = sseSessionService;
    }

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String PERMISSION_ADMIN_MANAGE = "admin:manage";
    private static final String ACTION_USER_CREATE = "USER_CREATE";
    private static final String ACTION_USER_UPDATE = "USER_UPDATE";
    private static final String ACTION_USER_RESET_PASSWORD = "USER_RESET_PASSWORD";
    private static final String ACTION_USER_CHANGE_OWN_PASSWORD = "USER_CHANGE_OWN_PASSWORD";
    private static final String ACTION_USER_DELETE = "USER_DELETE";

    /** 取得所有使用者 */
    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 搜尋使用者（支援分頁 + 多欄位模糊搜尋）。
     * 加入空條件攔截，確保安全性。
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(UserSearchRequest request, Pageable pageable) {
        // 1. 安全攔截：若搜尋條件皆為空，拋出 400 錯誤
        if (isEmptySearch(request)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "搜尋條件不可全為空，請至少提供一項搜尋欄位"
            );
        }

        // 2. 正規化分頁參數
        Pageable safePageable = normalizePageable(pageable);

        // 3. 建立規格並查詢
        Specification<User> spec = buildUserSpec(request);

        try {
            Page<User> page = userRepository.findAll(spec, safePageable);

            // 4. 若查無資料，回傳 404 (與 Supplier 風格一致)
            if (page.isEmpty()) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "查無匹配的使用者資料"
                );
            }

            return page.map(userMapper::toDto);
        } catch (org.springframework.data.mapping.PropertyReferenceException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "無效排序欄位：" + ex.getPropertyName()
            );
        }
    }

    /**
     * 🌿 新增空條件檢查工具方法
     * 包含字串欄位的 hasText 檢查，以及 Boolean 欄位的 null 檢查
     */
    private boolean isEmptySearch(UserSearchRequest req) {
        return !hasText(req.getUsername()) &&
                !hasText(req.getFullName()) &&
                !hasText(req.getEmail()) &&
                req.getEnabled() == null; // 確保「只選啟用/停用」時不會被判定為空搜尋
    }

    private Specification<User> buildUserSpec(UserSearchRequest req) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (hasText(req.getUsername())) {
                predicates.add(cb.like(
                        cb.lower(root.get("username")),
                        "%" + req.getUsername().trim().toLowerCase() + "%"
                ));
            }

            if (hasText(req.getFullName())) {
                predicates.add(cb.like(
                        cb.lower(root.get("fullName")),
                        "%" + req.getFullName().trim().toLowerCase() + "%"
                ));
            }

            if (hasText(req.getEmail())) {
                predicates.add(cb.like(
                        cb.lower(root.get("email")),
                        "%" + req.getEmail().trim().toLowerCase() + "%"
                ));
            }

            if (req.getEnabled() != null) {
                predicates.add(cb.equal(root.get("enabled"), req.getEnabled()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private Pageable normalizePageable(Pageable pageable) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize() <= 0 || pageable.getPageSize() > 200 ? 20 : pageable.getPageSize();
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.ASC, "id");
        return PageRequest.of(page, size, sort);
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

    /** 管理員建立使用者 - 🌿 引用 existsByUsername & existsByEmail 讓燈號亮起；寫入稽核 USER_CREATE。具管理員角色時需 admin:manage 權限。 */
    @Override
    @Transactional
    public UserDto createUser(UserRequestDto dto, Long currentUserId) {
        // 若欲賦予管理員角色，僅具 admin:manage（SUPER_ADMIN）者可執行
        if (dto.getRoleNames() != null && dto.getRoleNames().stream()
                .anyMatch(name -> name != null && (ROLE_ADMIN.equals(name.toUpperCase()) || ROLE_SUPER_ADMIN.equals(name.toUpperCase())))) {
            if (!SecurityUtils.hasAuthority(PERMISSION_ADMIN_MANAGE)) {
                throw new AccessDeniedException("僅超級管理員可建立具系統管理員或超級管理員角色的帳號。");
            }
        }

        // 0. 建立使用者時密碼必填
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("建立使用者時密碼為必填");
        }
        passwordPolicyValidator.validate(dto.getPassword());
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

        User saved = userRepository.save(user);
        if (currentUserId != null) {
            saveAudit(currentUserId, saved.getId(), ACTION_USER_CREATE,
                    "{\"username\":\"" + escapeJson(saved.getUsername()) + "\"}");
        }
        return userMapper.toDto(saved);
    }

    /** 使用者註冊 - 🌿 引用 existsByUsername 確保註冊安全 */
    @Override
    @Transactional
    public UserDto registerUser(UserRegisterDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("該帳號名稱已被註冊");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email 已被使用");
        }

        passwordPolicyValidator.validate(dto.getPassword());

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new EntityNotFoundException("Default role ROLE_USER not found"));

        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .enabled(true)
                .roles(new HashSet<>())
                .build();

        user.addRole(defaultRole);
        return userMapper.toDto(userRepository.save(user));
    }

    /** 更新使用者資訊（§4.1：R1 自己不可改角色/啟用、R2 保護最後一位管理員、僅具 admin:manage 者可修改其他管理員、稽核） */
    @Override
    @Transactional
    public UserDto updateUser(Long id, UserRequestDto dto, Long currentUserId) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        // 目標為管理員（ROLE_ADMIN 或 ROLE_SUPER_ADMIN）時，僅具 admin:manage 者可修改
        if (isAdminUser(user) && !SecurityUtils.hasAuthority(PERMISSION_ADMIN_MANAGE)) {
            throw new AccessDeniedException("僅超級管理員可修改其他系統管理員的資訊。");
        }

        // R1：自己改自己時，不允許更新 roleNames、enabled
        if (currentUserId != null && currentUserId.equals(id)) {
            if (dto.getRoleNames() != null || dto.getEnabled() != null) {
                throw new IllegalStateException("不可變更自己的角色或啟用狀態，請使用個人資料／修改密碼頁修改。");
            }
        }

        // R2：若會將此使用者從「啟用中管理員」改為非管理員或停用，須確保至少還有一位啟用中管理員（含 SUPER_ADMIN）
        boolean isCurrentlyEnabledAdmin = isAdminUser(user) && Boolean.TRUE.equals(user.getEnabled());
        boolean changingToNonAdmin = dto.getRoleNames() != null && !dto.getRoleNames().stream()
                .anyMatch(name -> name != null && (ROLE_ADMIN.equals(name.toUpperCase()) || ROLE_SUPER_ADMIN.equals(name.toUpperCase())));
        boolean changingToDisabled = Boolean.FALSE.equals(dto.getEnabled());
        if (isCurrentlyEnabledAdmin && (changingToNonAdmin || changingToDisabled)) {
            if (userRepository.countEnabledUsersWithAnyAdminRoleExcluding(id) < 1) {
                throw new IllegalStateException("系統至少需保留一位啟用中的系統管理員。");
            }
        }

        // 記錄變更前狀態供稽核 details
        String oldFullName = user.getFullName();
        Boolean oldEnabled = user.getEnabled();
        Set<String> oldRoleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        boolean passwordReset = dto.getPassword() != null && !dto.getPassword().isBlank();

        // 若修改 Email，需檢查是否與他人重複
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("新的 Email 已經被其他帳號使用");
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new IllegalArgumentException("新的帳號名稱已被其他帳號使用");
            }
            user.setUsername(dto.getUsername());
        }
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getEnabled() != null) user.setEnabled(dto.getEnabled());

        if (passwordReset) {
            passwordPolicyValidator.validate(dto.getPassword());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getRoleNames() != null) {
            Set<Role> newRoles = dto.getRoleNames().stream()
                    .map(name -> roleRepository.findByName(name.toUpperCase())
                            .orElseThrow(() -> new EntityNotFoundException("Role not found: " + name)))
                    .collect(Collectors.toSet());
            user.setRoles(newRoles);
        }

        User saved = userRepository.save(user);

        // 稽核：USER_UPDATE（含變更欄位新舊值；密碼僅記 "reset"）
        Map<String, Object> detailsMap = new LinkedHashMap<>();
        if (dto.getFullName() != null && !Objects.equals(oldFullName, dto.getFullName())) {
            detailsMap.put("fullName", Map.of("old", oldFullName != null ? oldFullName : "", "new", dto.getFullName()));
        }
        if (dto.getEnabled() != null && !Objects.equals(oldEnabled, dto.getEnabled())) {
            detailsMap.put("enabled", Map.of("old", oldEnabled != null ? oldEnabled : false, "new", dto.getEnabled()));
        }
        if (dto.getRoleNames() != null) {
            Set<String> newRoleNames = dto.getRoleNames().stream().map(String::toUpperCase).collect(Collectors.toSet());
            if (!newRoleNames.equals(oldRoleNames)) {
                detailsMap.put("roleNames", Map.of("old", oldRoleNames, "new", newRoleNames));
            }
        }
        if (passwordReset) {
            detailsMap.put("password", "reset");
        }
        String action = passwordReset && detailsMap.size() == 1 ? ACTION_USER_RESET_PASSWORD : ACTION_USER_UPDATE;
        saveAudit(currentUserId != null ? currentUserId : id, id, action, toDetailsJson(detailsMap));

        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, Long currentUserId) {
        // D1：不可刪除自己
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new IllegalStateException("不可刪除自己的帳號。");
        }

        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        // 目標為管理員時，僅具 admin:manage 者可刪除
        if (isAdminUser(user) && !SecurityUtils.hasAuthority(PERMISSION_ADMIN_MANAGE)) {
            throw new AccessDeniedException("僅超級管理員可刪除其他系統管理員帳號。");
        }

        // D2：不可刪除最後一位啟用中的系統管理員（含 ROLE_ADMIN / ROLE_SUPER_ADMIN）
        boolean isEnabledAdmin = isAdminUser(user) && Boolean.TRUE.equals(user.getEnabled());
        if (isEnabledAdmin && userRepository.countEnabledUsersWithAnyAdminRole() <= 1) {
            throw new IllegalStateException("不可刪除最後一位系統管理員。");
        }

        String targetUsername = user.getUsername();
        userRepository.delete(user);

        String details = "{\"targetUsername\":\"" + escapeJson(targetUsername) + "\"}";
        saveAudit(currentUserId != null ? currentUserId : id, id, ACTION_USER_DELETE, details);
    }

    @Override
    @Transactional
    public void changePasswordForCurrentUser(Long currentUserId, String currentPassword, String newPassword) {
        User user = userRepository.findByIdWithRoles(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUserId));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalStateException("目前密碼錯誤");
        }
        passwordPolicyValidator.validate(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setCredentialsChangedAt(LocalDateTime.now());
        userRepository.save(user);
        saveAudit(currentUserId, currentUserId, ACTION_USER_CHANGE_OWN_PASSWORD, "{\"password\":\"changed\"}");
    }

    @Override
    @Transactional
    public void updateLastLoginAt(Long userId) {
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void forceLogoutUser(Long targetUserId, Long operatorUserId) {
        // 僅超級管理員可強制登出其他系統管理員或一般使用者
        if (!SecurityUtils.hasRole("SUPER_ADMIN")) {
            throw new AccessDeniedException("僅超級管理員可執行強制登出。");
        }

        User user = userRepository.findByIdWithRoles(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + targetUserId));

        // 撤銷目標使用者所有 Refresh Token，並更新 credentialsChangedAt 使 Access Token 立即失效
        refreshTokenService.revokeAllForUser(targetUserId);
        user.setCredentialsChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // 即時推送 FORCE_LOGOUT 事件給該使用者的所有 SSE 連線
        sseSessionService.sendForceLogout(targetUserId);

        // 記錄稽核
        String details = "{\"action\":\"FORCE_LOGOUT\"}";
        saveAudit(operatorUserId != null ? operatorUserId : targetUserId, targetUserId, ACTION_USER_UPDATE, details);
    }

    private void saveAudit(Long operatorId, Long targetUserId, String action, String details) {
        userAuditLogRepository.save(UserAuditLog.builder()
                .occurredAt(Instant.now())
                .operatorId(operatorId)
                .targetUserId(targetUserId)
                .action(action)
                .details(details)
                .build());
    }

    private String toDetailsJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return map.toString();
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** 是否為系統管理員（具 ROLE_ADMIN 或 ROLE_SUPER_ADMIN） */
    private boolean isAdminUser(User user) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream()
                .anyMatch(r -> ROLE_ADMIN.equals(r.getName()) || ROLE_SUPER_ADMIN.equals(r.getName()));
    }
}