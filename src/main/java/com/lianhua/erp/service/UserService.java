    package com.lianhua.erp.service;

    import com.lianhua.erp.dto.user.UserDto;
    import com.lianhua.erp.dto.user.UserRegisterDto;
    import com.lianhua.erp.dto.user.UserRequestDto;
    import java.util.List;

    public interface UserService {

        List<UserDto> getAllUsers();

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
}
