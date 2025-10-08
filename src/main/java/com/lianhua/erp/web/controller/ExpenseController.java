package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.expense.ExpenseDto;
import com.lianhua.erp.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@Tag(name = "開支管理", description = "管理開支的 CRUD API")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    @Operation(summary = "取得所有開支", description = "回傳所有開支清單")
    public List<ExpenseDto> getAllExpenses() {
        return expenseService.getAllExpenses();
    }

    @GetMapping("/{id}")
    @Operation(summary = "取得單筆開支", description = "根據 ID 取得開支")
    public ExpenseDto getExpenseById(@PathVariable Long id) {
        return expenseService.getExpenseById(id);
    }

    @PostMapping
    @Operation(summary = "新增開支", description = "建立一筆新的開支")
    public ExpenseDto createExpense(@RequestBody ExpenseDto dto) {
        return expenseService.createExpense(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新開支", description = "更新一筆現有的開支")
    public ExpenseDto updateExpense(@PathVariable Long id, @RequestBody ExpenseDto dto) {
        return expenseService.updateExpense(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除開支", description = "刪除指定 ID 的開支")
    public void deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
    }
}
