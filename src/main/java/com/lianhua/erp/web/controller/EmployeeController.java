package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.EmployeeDto;
import com.lianhua.erp.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "員工管理", description = "管理員工的 CRUD API")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @Operation(summary = "取得所有員工", description = "回傳所有員工清單")
    public List<EmployeeDto> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/{id}")
    @Operation(summary = "取得單筆員工", description = "根據 ID 取得員工")
    public EmployeeDto getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    @PostMapping
    @Operation(summary = "新增員工", description = "建立一位新的員工")
    public EmployeeDto createEmployee(@RequestBody EmployeeDto dto) {
        return employeeService.createEmployee(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新員工", description = "更新一筆現有的員工資料")
    public EmployeeDto updateEmployee(@PathVariable Long id, @RequestBody EmployeeDto dto) {
        return employeeService.updateEmployee(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除員工", description = "刪除指定 ID 的員工")
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
    }
}
