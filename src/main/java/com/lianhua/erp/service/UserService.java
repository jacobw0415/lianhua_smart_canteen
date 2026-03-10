    package com.lianhua.erp.service;

    import com.lianhua.erp.dto.user.UserDto;
    import com.lianhua.erp.dto.user.UserRegisterDto;
    import com.lianhua.erp.dto.user.UserRequestDto;
    import com.lianhua.erp.dto.user.UserSearchRequest;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import java.util.List;
    
public interface UserService {

    List<UserDto> getAllUsers();

    /**
     * 搜尋使用者（支援分頁 + 多欄位模糊搜尋）。
     */
    Page<UserDto> searchUsers(UserSearchRequest request, Pageable pageable);

    UserDto getUserById(Long id);

    UserDto registerUser(UserRegisterDto dto);

    UserDto createUser(UserRequestDto dto, Long currentUserId);

    /** 更新使用者（含業務規則 R1、R2 與稽核）；currentUserId 為 JWT 當前使用者 id */
    UserDto updateUser(Long id, UserRequestDto dto, Long currentUserId);

    UserDto getUserByUsername(String username);

    /** 刪除使用者（含業務規則 D1、D2 與稽核）；currentUserId 為 JWT 當前使用者 id */
    void deleteUser(Long id, Long currentUserId);

    /** 本人修改密碼（驗證目前密碼後更新，寫入稽核 USER_CHANGE_OWN_PASSWORD） */
    void changePasswordForCurrentUser(Long currentUserId, String currentPassword, String newPassword);

    /** 登入成功時更新該使用者的最後登入時間（last_login_at） */
    void updateLastLoginAt(Long userId);

    /**
     * 強制指定使用者的所有存取憑證失效：
     * - 撤銷該使用者所有 Refresh Token
     * - 更新 credentialsChangedAt，讓既有 Access Token 也立即失效
     */
    void forceLogoutUser(Long targetUserId, Long operatorUserId);
}
