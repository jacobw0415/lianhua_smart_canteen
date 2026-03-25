package com.lianhua.erp.service.impl;

import com.lianhua.erp.domain.*;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.dto.expense.*;
import com.lianhua.erp.export.ExportDisplayZh;
import com.lianhua.erp.export.ExportFilenameUtils;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import com.lianhua.erp.export.TabularExporter;
import com.lianhua.erp.mapper.ExpenseMapper;
import com.lianhua.erp.repository.*;
import com.lianhua.erp.service.ExpenseService;
import com.lianhua.erp.service.impl.spec.ExpenseSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository repository;
    private final ExpenseCategoryRepository categoryRepository;
    private final EmployeeRepository employeeRepository;
    private final ExpenseMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    // ✅ 統一格式化器（會計期間）
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final String[] EXPENSE_EXPORT_HEADERS = new String[]{
            "支出日期",
            "會計期間",
            "費用類別",
            "支出金額",
            "員工",
            "狀態",
            "備註"
    };

    @org.springframework.beans.factory.annotation.Value("${app.export.max-rows:50000}")
    private int maxExportRows;

    /**
     * 計算指定日期所在週的開始日期（週一）和結束日期（週日）
     */
    private static LocalDate[] getWeekRange(LocalDate date) {
        // 計算週一（該週的第一天）
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        LocalDate weekStart = date.minusDays(dayOfWeek - 1);
        // 計算週日（該週的最後一天）
        LocalDate weekEnd = weekStart.plusDays(6);
        return new LocalDate[] { weekStart, weekEnd };
    }

    /**
     * 計算指定日期所在兩週週期的開始日期和結束日期
     * 兩週週期：以年份的第一個週一為起點，每14天為一個週期
     */
    private static LocalDate[] getBiweeklyRange(LocalDate date) {
        // 計算該年份的第一個週一
        LocalDate jan1 = LocalDate.of(date.getYear(), 1, 1);
        int dayOfWeek = jan1.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        LocalDate firstMonday = dayOfWeek == 1 ? jan1 : jan1.plusDays(8 - dayOfWeek);

        // 計算該日期距離第一個週一的天數
        long daysFromFirstMonday = java.time.temporal.ChronoUnit.DAYS.between(firstMonday, date);
        if (daysFromFirstMonday < 0) {
            // 如果該日期在第一個週一之前，使用上一年最後的兩週週期
            LocalDate prevYearLastMonday = LocalDate.of(date.getYear() - 1, 12, 31)
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            long daysFromPrevMonday = java.time.temporal.ChronoUnit.DAYS.between(prevYearLastMonday, date);
            long periodNumber = daysFromPrevMonday / 14;
            LocalDate periodStart = prevYearLastMonday.plusDays(periodNumber * 14);
            LocalDate periodEnd = periodStart.plusDays(13);
            return new LocalDate[] { periodStart, periodEnd };
        }

        // 計算該日期所在的兩週週期（每14天一個週期）
        long periodNumber = daysFromFirstMonday / 14;
        LocalDate periodStart = firstMonday.plusDays(periodNumber * 14);
        LocalDate periodEnd = periodStart.plusDays(13); // 14天週期：第1天到第14天
        return new LocalDate[] { periodStart, periodEnd };
    }

    @Override
    @Transactional
    public ExpenseDto create(ExpenseRequestDto dto) {
        ExpenseCategory category;
        Employee employee = null;

        // === 如果指定了員工 ID，優先處理員工相關邏輯 ===
        if (dto.getEmployeeId() != null) {
            employee = employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID: " + dto.getEmployeeId()));

            // ✅ 檢查員工狀態（必須為 ACTIVE）
            if (employee.getStatus() != Employee.Status.ACTIVE) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("員工「%s」狀態為 %s，僅能為啟用中（ACTIVE）的員工建立薪資支出記錄。",
                                employee.getFullName(), employee.getStatus()));
            }

            // 檢查員工是否有薪資設定
            if (employee.getSalary() == null || employee.getSalary().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("員工「%s」未設定薪資或薪資為 0，無法建立薪資支出記錄。", employee.getFullName()));
            }

            // 如果指定了類別 ID，檢查類別是否為薪資類別
            if (dto.getCategoryId() != null) {
                category = categoryRepository.findById(dto.getCategoryId())
                        .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID: " + dto.getCategoryId()));

                // ✅ 檢查類別是否為薪資類別
                if (!Boolean.TRUE.equals(category.getIsSalary())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            String.format("費用類別「%s」非薪資類別，選擇員工時必須使用薪資類別。", category.getName()));
                }

                // ✅ 檢查類別是否啟用（提前檢查，避免後續不必要的處理）
                if (Boolean.FALSE.equals(category.getActive())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "費用類別「" + category.getName() + "」已停用，無法使用。");
                }
            } else {
                // 如果沒有指定類別 ID，自動查找薪資類別
                // ✅ 使用 Spring Data JPA 方法名查詢（更可靠，兼容性更好）
                List<ExpenseCategory> salaryCategories = categoryRepository
                        .findByActiveTrueAndIsSalaryTrueOrderByNameAsc();

                // ✅ 添加詳細調試日誌
                log.debug("查詢啟用中的薪資類別，找到 {} 筆記錄", salaryCategories.size());

                if (salaryCategories.isEmpty()) {
                    // ✅ 添加更詳細的錯誤信息，幫助調試
                    long totalCategories = categoryRepository.count();
                    List<ExpenseCategory> allActiveCategories = categoryRepository
                            .findAllByActiveTrueOrderByAccountCodeAsc();
                    long activeCategories = allActiveCategories.size();

                    // 列出所有啟用的類別，檢查 isSalary 狀態
                    log.warn("找不到啟用中的薪資類別。總類別數: {}, 啟用類別數: {}", totalCategories, activeCategories);
                    log.warn("啟用中的類別詳細信息:");
                    for (ExpenseCategory cat : allActiveCategories) {
                        log.warn("  - 類別: {} (ID: {}), active: {}, isSalary: {}",
                                cat.getName(), cat.getId(), cat.getActive(), cat.getIsSalary());
                    }

                    // 也查詢所有類別（包括停用的），看是否有 isSalary=true 但 active=false 的情況
                    List<ExpenseCategory> allCategories = categoryRepository.findAllByOrderByAccountCodeAsc();
                    log.warn("所有類別（包括停用）的 isSalary 狀態:");
                    for (ExpenseCategory cat : allCategories) {
                        if (Boolean.TRUE.equals(cat.getIsSalary())) {
                            log.warn("  - 薪資類別: {} (ID: {}), active: {}, isSalary: {}",
                                    cat.getName(), cat.getId(), cat.getActive(), cat.getIsSalary());
                        }
                    }

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "系統中沒有啟用中的薪資類別，請先至「費用類別管理」新增並啟用薪資類別。");
                }
                // 選擇第一個找到的薪資類別（通常應該只有一個）
                category = salaryCategories.get(0);
                dto.setCategoryId(category.getId());
                log.info("自動選擇薪資類別: {} (ID: {}), active: {}, isSalary: {}",
                        category.getName(), category.getId(), category.getActive(), category.getIsSalary());
            }

            // ✅ 自動設定金額為員工薪資
            dto.setAmount(employee.getSalary());
            log.info("自動設定金額為員工薪資: {} (員工: {})", employee.getSalary(), employee.getFullName());
        } else {
            // === 如果沒有指定員工，使用指定的類別 ===
            if (dto.getCategoryId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "未指定費用類別 ID，請選擇費用類別。");
            }

            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID: " + dto.getCategoryId()));

            // 如果類別是薪資類別但沒有指定員工，報錯
            if (Boolean.TRUE.equals(category.getIsSalary())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("費用類別「%s」為薪資類別，必須指定員工 ID。", category.getName()));
            }

            // ✅ 檢查類別是否啟用（提前檢查，避免後續不必要的處理）
            if (Boolean.FALSE.equals(category.getActive())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "費用類別「" + category.getName() + "」已停用，無法使用。");
            }

            // === 未選擇員工時，必須手動輸入金額 ===
            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "未選擇員工時，支出金額為必填且必須大於 0。");
            }
        }

        // === 最終金額驗證（選擇員工時應該已經設置，這裡只是再次確認）===
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "支出金額必須大於 0。");
        }

        // === 計算會計期間（用於頻率檢查）===
        LocalDate expenseDate = dto.getExpenseDate();
        String accountingPeriod = expenseDate != null ? expenseDate.format(PERIOD_FORMAT)
                : LocalDate.now().format(PERIOD_FORMAT);

        // === 根據費用類別的頻率類型檢查是否已存在重複的支出記錄 ===
        Long categoryId = category.getId();
        ExpenseFrequency frequencyType = category.getFrequencyType() != null ? category.getFrequencyType()
                : ExpenseFrequency.DAILY;
        boolean isDuplicate;
        String errorMessage = null;

        if (employee != null) {
            // 薪資類別：根據頻率類型檢查
            switch (frequencyType) {
                case MONTHLY:
                    // 每月一次：檢查同一會計期間是否已有記錄
                    isDuplicate = repository.existsByEmployeeIdAndAccountingPeriodAndCategoryId(
                            employee.getId(), accountingPeriod, categoryId, ExpenseStatus.ACTIVE);
                    if (isDuplicate) {
                        errorMessage = String.format("員工「%s」在會計期間 %s 已有費用類別「%s」的支出記錄，該類別設定為每月一次，同一會計期間只能建立一筆記錄。",
                                employee.getFullName(), accountingPeriod, category.getName());
                    }
                    break;
                case WEEKLY:
                    // 每週一次：檢查同一週是否已有記錄
                    LocalDate[] weekRange = getWeekRange(expenseDate);
                    isDuplicate = repository.existsByEmployeeIdAndExpenseDateBetweenAndCategoryId(
                            employee.getId(), weekRange[0], weekRange[1], categoryId, ExpenseStatus.ACTIVE);
                    if (isDuplicate) {
                        errorMessage = String.format("員工「%s」在 %s 至 %s 的週期內已有費用類別「%s」的支出記錄，該類別設定為每週一次，同一週只能建立一筆記錄。",
                                employee.getFullName(), weekRange[0], weekRange[1], category.getName());
                    }
                    break;
                case BIWEEKLY:
                    // 每兩週一次：檢查同一兩週週期是否已有記錄
                    LocalDate[] biweeklyRange = getBiweeklyRange(expenseDate);
                    isDuplicate = repository.existsByEmployeeIdAndExpenseDateBetweenAndCategoryId(
                            employee.getId(), biweeklyRange[0], biweeklyRange[1], categoryId, ExpenseStatus.ACTIVE);
                    if (isDuplicate) {
                        errorMessage = String.format("員工「%s」在 %s 至 %s 的週期內已有費用類別「%s」的支出記錄，該類別設定為每兩週一次，同一兩週週期只能建立一筆記錄。",
                                employee.getFullName(), biweeklyRange[0], biweeklyRange[1], category.getName());
                    }
                    break;
                case DAILY:
                case UNLIMITED:
                default:
                    // 每日一次或無限制：檢查同一天是否已有記錄
                    isDuplicate = repository.existsByEmployeeIdAndExpenseDateAndCategoryId(
                            employee.getId(), expenseDate, categoryId, ExpenseStatus.ACTIVE);
                    if (isDuplicate) {
                        errorMessage = String.format("員工「%s」在 %s 已有費用類別「%s」的支出記錄，同一天同一類別只能建立一筆記錄。",
                                employee.getFullName(), expenseDate, category.getName());
                    }
                    break;
            }
        } else {
            // 非薪資類別：根據頻率類型檢查
            switch (frequencyType) {
                case MONTHLY:
                    // 每月一次：檢查同一會計期間是否已有記錄
                    isDuplicate = repository.existsByEmployeeIdIsNullAndAccountingPeriodAndCategoryId(
                            accountingPeriod, categoryId, ExpenseStatus.ACTIVE);
                    if (isDuplicate) {
                        errorMessage = String.format("在會計期間 %s 已有費用類別「%s」的支出記錄，該類別設定為每月一次，同一會計期間只能建立一筆記錄。",
                                accountingPeriod, category.getName());
                    }
                    break;
                case WEEKLY:
                    // 每週一次：檢查同一週是否已有記錄
                    LocalDate[] weekRange = getWeekRange(expenseDate);
                    isDuplicate = repository.existsByEmployeeIdIsNullAndExpenseDateBetweenAndCategoryId(
                            weekRange[0], weekRange[1], categoryId, ExpenseStatus.ACTIVE);
                    if (isDuplicate) {
                        errorMessage = String.format("在 %s 至 %s 的週期內已有費用類別「%s」的支出記錄，該類別設定為每週一次，同一週只能建立一筆記錄。",
                                weekRange[0], weekRange[1], category.getName());
                    }
                    break;
                case BIWEEKLY:
                    // 每兩週一次：檢查同一兩週週期是否已有記錄
                    LocalDate[] biweeklyRange = getBiweeklyRange(expenseDate);
                    isDuplicate = repository.existsByEmployeeIdIsNullAndExpenseDateBetweenAndCategoryId(
                            biweeklyRange[0], biweeklyRange[1], categoryId, ExpenseStatus.ACTIVE);
                    if (isDuplicate) {
                        errorMessage = String.format("在 %s 至 %s 的週期內已有費用類別「%s」的支出記錄，該類別設定為每兩週一次，同一兩週週期只能建立一筆記錄。",
                                biweeklyRange[0], biweeklyRange[1], category.getName());
                    }
                    break;
                case DAILY:
                case UNLIMITED:
                default:
                    // 每日一次或無限制：檢查同一天是否已有記錄
                    isDuplicate = repository.existsByEmployeeIdIsNullAndExpenseDateAndCategoryId(
                            expenseDate, categoryId, ExpenseStatus.ACTIVE);
                    if (isDuplicate) {
                        errorMessage = String.format("在 %s 已有費用類別「%s」的支出記錄，同一天同一類別只能建立一筆記錄。",
                                expenseDate, category.getName());
                    }
                    break;
            }
        }

        if (errorMessage != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    errorMessage);
        }

        Expense expense = mapper.toEntity(dto);
        expense.setCategory(category);

        if (employee != null) {
            expense.setEmployee(employee);
        }

        // ✅ 自動設定會計期間（依 expenseDate 為準）
        expense.setAccountingPeriod(accountingPeriod);

        // ✨ 新增：明確設定初始狀態為 ACTIVE，確保新建立的紀錄能被下次檢查偵測到
        expense.setStatus(ExpenseStatus.ACTIVE);

        Expense saved = repository.save(expense);
        log.info("成功創建費用: ID={}, 類別={}, 金額={}, 日期={}",
                saved.getId(), saved.getCategory().getName(), saved.getAmount(), saved.getExpenseDate());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public ExpenseDto update(Long id, ExpenseRequestDto dto) {
        Expense entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到支出 ID: " + id));

        // ✅ 檢查是否已作廢（已作廢的記錄不能編輯）
        if (entity.getStatus() == ExpenseStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "此支出記錄已作廢，無法編輯。如需更正，請作廢後建立新記錄。");
        }

        // 開支日期不可修改（防止報表錯位）
        if (dto.getExpenseDate() == null || !entity.getExpenseDate().equals(dto.getExpenseDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "開支日期不可修改，若需異動請建立新紀錄。");
        }

        // 類別若要修改，必須存在且不違反薪資邏輯
        if (dto.getCategoryId() != null && !dto.getCategoryId().equals(entity.getCategory().getId())) {
            ExpenseCategory newCategory = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到費用類別 ID: " + dto.getCategoryId()));

            // === 檢查新分類是否啟用 ===
            if (Boolean.FALSE.equals(newCategory.getActive())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "費用類別「" + newCategory.getName() + "」已停用，無法使用。");
            }

            // 若類別變動涉及薪資員工，需額外防呆
            boolean wasSalary = Boolean.TRUE.equals(entity.getCategory().getIsSalary());
            boolean nowSalary = Boolean.TRUE.equals(newCategory.getIsSalary());
            if (wasSalary != nowSalary) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "不可將薪資類支出改為非薪資類（或反之）。");
            }

            entity.setCategory(newCategory);
        }

        // 員工關聯僅在薪資類別可修改
        if (dto.getEmployeeId() != null) {
            if (!Boolean.TRUE.equals(entity.getCategory().getIsSalary())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("僅薪資類別可設定員工，當前類別為「%s」。", entity.getCategory().getName()));
            }
            Employee employee = employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到員工 ID: " + dto.getEmployeeId()));

            // ✅ 檢查員工狀態（必須為 ACTIVE）
            if (employee.getStatus() != Employee.Status.ACTIVE) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("員工「%s」狀態為 %s，僅能為啟用中（ACTIVE）的員工設定薪資支出。",
                                employee.getFullName(), employee.getStatus()));
            }

            entity.setEmployee(employee);
        }

        // ✅ 薪資類別的支出金額不可修改
        if (Boolean.TRUE.equals(entity.getCategory().getIsSalary())) {
            if (dto.getAmount() != null && entity.getAmount().compareTo(dto.getAmount()) != 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "薪資類別的支出金額不可修改。如需調整薪資，請先更新員工的薪資設定，然後作廢此記錄並重新建立。");
            }
        }

        // 若非薪資類別，金額異動需記錄警示（未建立 Audit Table 可先記 Log）
        if (!Boolean.TRUE.equals(entity.getCategory().getIsSalary())
                && dto.getAmount() != null && entity.getAmount().compareTo(dto.getAmount()) != 0) {
            log.warn("💰 開支金額異動：ID={} | 原金額={} | 新金額={}",
                    id, entity.getAmount(), dto.getAmount());
        }

        // ✅ 可自由修改的欄位（薪資類別的金額已在上方被鎖定）
        if (dto.getAmount() != null && !Boolean.TRUE.equals(entity.getCategory().getIsSalary())) {
            entity.setAmount(dto.getAmount());
        }

        entity.setNote(dto.getNote());

        // ✅ accountingPeriod 不可修改，故此處不動

        Expense updated = repository.save(entity);
        log.info("成功更新費用: ID={}, 類別={}, 金額={}",
                updated.getId(), updated.getCategory().getName(), updated.getAmount());
        return mapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseDto findById(Long id) {
        return repository.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("找不到支出 ID: " + id));
    }

    @Override
    @Transactional
    public ExpenseDto voidExpense(Long id, String reason) {
        Expense expense = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到支出 ID: " + id));

        // 檢查是否已作廢
        if (expense.getStatus() == ExpenseStatus.VOIDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "此支出記錄已經作廢");
        }

        // 標記為已作廢
        expense.setStatus(ExpenseStatus.VOIDED);
        expense.setVoidedAt(LocalDateTime.now());
        expense.setVoidReason(reason);

        Expense saved = repository.save(expense);

        // 🚀 ✨ 新增：發送「費用作廢」通知 (對齊三行格式：單號、金額、原因)
        Map<String, Object> payload = new java.util.HashMap<>();
        String categoryName = saved.getCategory() != null ? saved.getCategory().getName() : "未知分類";

        // 這裡的 Key "no", "amount", "reason" 必須與 NotificationServiceImpl 對齊
        payload.put("no", "EXP-00" + saved.getId() + " (" + categoryName + ")");
        payload.put("amount", saved.getAmount());
        payload.put("reason", reason);

        log.info("🚀 發送費用作廢事件：ID {}", saved.getId());
        eventPublisher.publishEvent(new com.lianhua.erp.event.ExpenseEvent(this, saved, "EXPENSE_VOIDED", payload));

        log.info("✅ 作廢支出記錄: expenseId={}, 類別={}, 金額={}, 原因={}",
                id, saved.getCategory().getName(), saved.getAmount(), reason);

        // 重新查詢以確保關聯資料被載入（用於映射 categoryName 和 employeeName）
        Expense updatedExpense = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到支出 ID: " + id));

        return mapper.toDto(updatedExpense);
    }

    // ============================================
    // 搜尋費用支出（支援模糊搜尋與分頁）
    // ============================================
    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseDto> searchExpenses(ExpenseSearchRequest req, Pageable pageable) {

        // === 檢查搜尋條件是否全為空（includeVoided 和 status 不計入搜尋條件）===
        boolean empty = isEmpty(req.getCategoryName()) &&
                req.getCategoryId() == null &&
                isEmpty(req.getEmployeeName()) &&
                req.getEmployeeId() == null &&
                isEmpty(req.getAccountingPeriod()) &&
                isEmpty(req.getFromDate()) &&
                isEmpty(req.getToDate()) &&
                isEmpty(req.getNote()) &&
                isEmpty(req.getStatus());

        if (empty) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜尋條件不可全為空，至少需提供一項搜尋欄位");
        }

        // === 建 Specification ===
        Specification<Expense> spec = ExpenseSpecifications.build(req);

        // === 執行查詢 ===
        try {
            Page<Expense> result = repository.findAll(spec, pageable);
            return result.map(mapper::toDto);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效排序欄位：" + ex.getPropertyName());
        }
    }

    // ============================================
    // 匯出支出紀錄（與 searchExpenses 相同條件）
    // ============================================
    @Override
    @Transactional(readOnly = true)
    public ExportPayload exportExpenses(
            ExpenseSearchRequest req,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    ) {
        ExpenseSearchRequest request = req == null ? new ExpenseSearchRequest() : req;

        ExportFormat safeFormat = format == null ? ExportFormat.XLSX : format;
        ExportScope safeScope = scope == null ? ExportScope.ALL : scope;

        Specification<Expense> spec = ExpenseSpecifications.build(request);

        Sort safeSort = pageable != null && pageable.getSort() != null && pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "expenseDate");

        List<String[]> rows;

        if (safeScope == ExportScope.ALL) {
            try {
                long total = repository.count(spec);
                if (total > maxExportRows) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "匯出筆數超過上限 (" + maxExportRows + ")，請縮小篩選條件");
                }
                rows = new ArrayList<>((int) Math.min(total, Integer.MAX_VALUE));

                int step = 1000;
                if (pageable != null && pageable.getPageSize() > 0 && pageable.getPageSize() <= 200) {
                    step = Math.max(50, pageable.getPageSize());
                }

                int pages = (int) ((total + step - 1) / step);
                for (int p = 0; p < pages; p++) {
                    Page<Expense> page = repository.findAll(spec, PageRequest.of(p, step, safeSort));
                    for (Expense e : page.getContent()) {
                        rows.add(toExpenseExportRow(mapper.toDto(e)));
                    }
                }
            } catch (PropertyReferenceException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "無效排序欄位：" + ex.getPropertyName());
            }
        } else {
            rows = new ArrayList<>();
            try {
                Pageable p = pageable == null
                        ? PageRequest.of(0, 25, safeSort)
                        : normalizeForExport(pageable, safeSort);
                Page<Expense> page = repository.findAll(spec, p);
                for (Expense e : page.getContent()) {
                    rows.add(toExpenseExportRow(mapper.toDto(e)));
                }
            } catch (PropertyReferenceException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "無效排序欄位：" + ex.getPropertyName());
            }
        }

        byte[] data = switch (safeFormat) {
            case XLSX -> TabularExporter.toXlsx("expenses", EXPENSE_EXPORT_HEADERS, rows);
            case CSV -> TabularExporter.toCsvUtf8Bom(EXPENSE_EXPORT_HEADERS, rows);
        };

        String filename = ExportFilenameUtils.build("expenses", safeFormat);
        return new ExportPayload(data, filename, safeFormat.mediaType());
    }

    private Pageable normalizeForExport(Pageable pageable, Sort safeSort) {
        if (pageable.getPageNumber() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 無效");
        }
        int size = pageable.getPageSize();
        if (size <= 0 || size > 200) {
            size = 25;
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }

    private static String[] toExpenseExportRow(ExpenseDto e) {
        return new String[]{
                e.getExpenseDate() == null ? "" : e.getExpenseDate().toString(),
                nz(e.getAccountingPeriod()),
                nz(e.getCategoryName()),
                e.getAmount() == null ? "" : e.getAmount().toPlainString(),
                nz(e.getEmployeeName()),
                e.getStatus() == null ? "" : ExportDisplayZh.recordActiveVoid(e.getStatus().name()),
                nz(e.getNote())
        };
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
