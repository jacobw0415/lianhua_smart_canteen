package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.EmployeeDto;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "員工管理", description = "管理員工的 CRUD API")
public class EmployeeController {
// Swagger UI 主頁面 → http://localhost:8080/swagger-ui.html
    @GetMapping
    @Operation(summary = "取得所有員工", description = "回傳所有員工清單")
    public List<EmployeeDto> getAllEmployees() {
        return List.of(); // TODO: service呼叫
    }

    @PostMapping
    @Operation(summary = "新增員工", description = "新增一位新的員工")
    public EmployeeDto createEmployee(@RequestBody EmployeeDto dto) {
        return dto; // TODO: service呼叫
    }
}

