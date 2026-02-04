    package com.lianhua.erp.service;

    import com.lianhua.erp.dto.user.UserDto;
    import com.lianhua.erp.dto.user.UserRegisterDto;
    import com.lianhua.erp.dto.user.UserRequestDto;
    import java.util.List;

    public interface UserService {

        List<UserDto> getAllUsers();

        UserDto getUserById(Long id);

        UserDto registerUser(UserRegisterDto dto);

        UserDto createUser(UserRequestDto dto);

        UserDto updateUser(Long id, UserRequestDto dto);

        UserDto getUserByUsername(String username);

        void deleteUser(Long id);
    }
