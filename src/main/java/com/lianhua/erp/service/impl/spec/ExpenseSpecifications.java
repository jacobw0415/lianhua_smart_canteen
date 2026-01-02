package com.lianhua.erp.service.impl.spec;

import com.lianhua.erp.domain.Expense;
import com.lianhua.erp.domain.ExpenseStatus;
import com.lianhua.erp.dto.expense.ExpenseSearchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;

public class ExpenseSpecifications {

  /**
   * 主方法：依照搜尋條件動態組合 Specification
   */
  public static Specification<Expense> build(ExpenseSearchRequest req) {
    Specification<Expense> spec = Specification.allOf();

    // ======================================================
    // ⭐ 狀態過濾（精確 + 模糊 + 字首推測）
    // ======================================================
    if (StringUtils.hasText(req.getStatus())) {
      ExpenseStatus targetStatus = resolveStatus(req.getStatus());

      if (targetStatus != null) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), targetStatus));
      }
    } else {
      // 預設排除已作廢（除非明確指定）
      if (!Boolean.TRUE.equals(req.getIncludeVoided())) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), ExpenseStatus.ACTIVE));
      }
    }

    spec = spec.and(byCategoryName(req));
    spec = spec.and(byCategoryId(req));
    spec = spec.and(byEmployeeName(req));
    spec = spec.and(byEmployeeId(req));
    spec = spec.and(byAccountingPeriod(req));
    spec = spec.and(byDateRange(req));
    spec = spec.and(byNote(req));

    return spec;
  }

  /*
   * ======================================================
   * ⭐ 狀態解析核心（支援精確匹配、模糊搜尋、字首推測）
   * ======================================================
   */
  private static ExpenseStatus resolveStatus(String input) {
    if (!StringUtils.hasText(input))
      return null;

    String normalized = input.trim().toLowerCase();

    // ---------- 1️⃣ 先嘗試 enum 精確匹配 ----------
    try {
      return ExpenseStatus.valueOf(normalized.toUpperCase());
    } catch (IllegalArgumentException ignore) {
      // 繼續做模糊與字首解析
    }

    // ---------- 2️⃣ 中文 / 英文模糊關鍵字 ----------
    if (containsAny(normalized, "作廢", "作废", "void", "voided")) {
      return ExpenseStatus.VOIDED;
    }

    if (containsAny(normalized, "有效", "正常", "生效", "active")) {
      return ExpenseStatus.ACTIVE;
    }

    // ---------- 3️⃣ 字首推測（ACT / VOI / VO）----------
    if (normalized.startsWith("act")) {
      return ExpenseStatus.ACTIVE;
    }

    if (normalized.startsWith("voi") || normalized.startsWith("vo")) {
      return ExpenseStatus.VOIDED;
    }

    return null; // 無法解析，返回 null（不添加狀態過濾）
  }

  /**
   * 檢查字串是否包含任一關鍵字
   */
  private static boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 費用類別名稱（模糊搜尋）
   */
  private static Specification<Expense> byCategoryName(ExpenseSearchRequest req) {
    if (isEmpty(req.getCategoryName())) {
      return null;
    }

    String keyword = "%" + req.getCategoryName().trim() + "%";

    return (root, query, cb) -> cb.like(
        root.join("category").get("name"),
        keyword);
  }

  /**
   * 費用類別 ID（精準搜尋）
   */
  private static Specification<Expense> byCategoryId(ExpenseSearchRequest req) {
    if (req.getCategoryId() == null) {
      return null;
    }

    return (root, query, cb) -> cb.equal(root.join("category").get("id"), req.getCategoryId());
  }

  /**
   * 員工名稱（模糊搜尋）
   * 當搜索員工姓名時，只返回有員工的支出記錄（employee IS NOT NULL）
   * 使用 INNER JOIN 確保只查詢有員工關聯的記錄
   */
  private static Specification<Expense> byEmployeeName(ExpenseSearchRequest req) {
    if (isEmpty(req.getEmployeeName())) {
      return null;
    }

    String keyword = "%" + req.getEmployeeName().trim() + "%";

    return (root, query, cb) -> {
      // 使用 INNER JOIN 確保只查詢有員工的記錄
      // 字段名是 fullName 而不是 name
      // 同時添加 employee IS NOT NULL 條件以確保安全
      return cb.and(
          cb.isNotNull(root.get("employee")),
          cb.like(
              cb.lower(root.join("employee", JoinType.INNER).get("fullName")),
              keyword.toLowerCase()));
    };
  }

  /**
   * 員工 ID（精準搜尋）
   * 使用 LEFT JOIN 以支持非薪资支出（employee 为 NULL）的情况
   */
  private static Specification<Expense> byEmployeeId(ExpenseSearchRequest req) {
    if (req.getEmployeeId() == null) {
      return null;
    }

    return (root, query, cb) -> cb.equal(root.join("employee", JoinType.LEFT).get("id"), req.getEmployeeId());
  }

  /**
   * 會計期間（精準搜尋）
   */
  private static Specification<Expense> byAccountingPeriod(ExpenseSearchRequest req) {
    if (isEmpty(req.getAccountingPeriod())) {
      return null;
    }

    return (root, query, cb) -> cb.equal(root.get("accountingPeriod"), req.getAccountingPeriod().trim());
  }

  /**
   * 支出日期範圍
   */
  private static Specification<Expense> byDateRange(ExpenseSearchRequest req) {
    Specification<Expense> spec = Specification.allOf();

    if (!isEmpty(req.getFromDate())) {
      LocalDate from = LocalDate.parse(req.getFromDate());
      spec = spec.and(
          (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from));
    }

    if (!isEmpty(req.getToDate())) {
      LocalDate to = LocalDate.parse(req.getToDate());
      spec = spec.and(
          (root, query, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to));
    }

    return spec;
  }

  /**
   * 備註（模糊搜尋）
   */
  private static Specification<Expense> byNote(ExpenseSearchRequest req) {
    if (isEmpty(req.getNote())) {
      return null;
    }

    String keyword = "%" + req.getNote().trim() + "%";

    return (root, query, cb) -> cb.like(root.get("note"), keyword);
  }

  private static boolean isEmpty(String s) {
    return s == null || s.trim().isEmpty();
  }
}
