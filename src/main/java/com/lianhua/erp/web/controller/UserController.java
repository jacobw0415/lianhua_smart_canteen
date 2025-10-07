package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.user.UserDto;
import com.lianhua.erp.dto.user.UserRequestDto;
import com.lianhua.erp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "使用者管理", description = "使用者與角色管理 API")
public class UserController {
    
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    @Operation(summary = "取得所有使用者（含角色）")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "取得指定使用者（含角色）")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
    
    @PostMapping
    @Operation(summary = "建立使用者（可指定角色）")
    public ResponseEntity<UserDto> createUser(@RequestBody UserRequestDto dto) {
        return ResponseEntity.ok(userService.createUser(dto));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新使用者資訊（可修改角色）")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserRequestDto dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "刪除使用者")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
