package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.report.*;
import com.lianhua.erp.service.ReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<SupplierBalanceReportDto> getSupplierBalances() {
        String sql = """
            SELECT new com.lianhua.erp.dto.report.SupplierBalanceReportDto(
                s.id, s.name,
                COALESCE(SUM(p.qty * p.unitPrice + p.tax), 0),
                COALESCE(SUM(pay.amount), 0),
                COALESCE(SUM(p.qty * p.unitPrice + p.tax), 0) - COALESCE(SUM(pay.amount), 0)
            )
            FROM Supplier s
            LEFT JOIN Purchase p ON s.id = p.supplier.id
            LEFT JOIN Payment pay ON p.id = pay.purchase.id
            GROUP BY s.id, s.name
            ORDER BY (COALESCE(SUM(p.qty * p.unitPrice + p.tax), 0) - COALESCE(SUM(pay.amount), 0)) DESC
        """;
        return entityManager.createQuery(sql, SupplierBalanceReportDto.class).getResultList();
    }

    @Override
    public List<SupplierMonthlyReportDto> getSupplierMonthlyReports() {
        String sql = """
            SELECT new com.lianhua.erp.dto.report.SupplierMonthlyReportDto(
                s.name,
                FUNCTION('DATE_FORMAT', p.purchaseDate, '%Y-%m'),
                COALESCE(SUM(p.qty * p.unitPrice + p.tax), 0),
                COALESCE(SUM(pay.amount), 0),
                COALESCE(SUM(p.qty * p.unitPrice + p.tax), 0) - COALESCE(SUM(pay.amount), 0)
            )
            FROM Supplier s
            LEFT JOIN Purchase p ON s.id = p.supplier.id
            LEFT JOIN Payment pay ON p.id = pay.purchase.id
            GROUP BY s.name, FUNCTION('DATE_FORMAT', p.purchaseDate, '%Y-%m')
            ORDER BY s.name, FUNCTION('DATE_FORMAT', p.purchaseDate, '%Y-%m')
        """;
        return entityManager.createQuery(sql, SupplierMonthlyReportDto.class).getResultList();
    }

    @Override
    public List<PurchasePaymentStatusDto> getPurchasePaymentStatuses() {
        String sql = """
            SELECT new com.lianhua.erp.dto.report.PurchasePaymentStatusDto(
                p.id, s.name, CAST(p.purchaseDate AS string),
                (p.qty * p.unitPrice + p.tax),
                COALESCE(SUM(pay.amount), 0),
                (p.qty * p.unitPrice + p.tax) - COALESCE(SUM(pay.amount), 0),
                p.status
            )
            FROM Purchase p
            JOIN Supplier s ON p.supplier.id = s.id
            LEFT JOIN Payment pay ON p.id = pay.purchase.id
            GROUP BY p.id, s.name, p.purchaseDate, p.qty, p.unitPrice, p.tax, p.status
            ORDER BY ((p.qty * p.unitPrice + p.tax) - COALESCE(SUM(pay.amount), 0)) DESC
        """;
        return entityManager.createQuery(sql, PurchasePaymentStatusDto.class).getResultList();
    }

    @Override
    public List<SupplierBalanceReportDto> getUnpaidSuppliers() {
        String sql = """
            SELECT new com.lianhua.erp.dto.report.SupplierBalanceReportDto(
                s.id, s.name,
                SUM(p.qty * p.unitPrice + p.tax),
                COALESCE(SUM(pay.amount), 0),
                SUM(p.qty * p.unitPrice + p.tax) - COALESCE(SUM(pay.amount), 0)
            )
            FROM Supplier s
            JOIN Purchase p ON s.id = p.supplier.id
            LEFT JOIN Payment pay ON p.id = pay.purchase.id
            GROUP BY s.id, s.name
            HAVING (SUM(p.qty * p.unitPrice + p.tax) - COALESCE(SUM(pay.amount), 0)) > 0
            ORDER BY (SUM(p.qty * p.unitPrice + p.tax) - COALESCE(SUM(pay.amount), 0)) DESC
        """;
        return entityManager.createQuery(sql, SupplierBalanceReportDto.class).getResultList();
    }

    // ==== 員工薪資相關 ====

    @Override
    public List<MonthlySalaryReportDto> getMonthlySalaryExpenses() {
        String sql = """
            SELECT new com.lianhua.erp.dto.report.MonthlySalaryReportDto(
                FUNCTION('DATE_FORMAT', e.expenseDate, '%Y-%m'),
                SUM(e.amount)
            )
            FROM Expense e
            WHERE e.employee IS NOT NULL
            GROUP BY FUNCTION('DATE_FORMAT', e.expenseDate, '%Y-%m')
            ORDER BY FUNCTION('DATE_FORMAT', e.expenseDate, '%Y-%m')
        """;
        return entityManager.createQuery(sql, MonthlySalaryReportDto.class).getResultList();
    }

    @Override
    public List<EmployeeYearlySalaryReportDto> getEmployeeYearlySalaries() {
        String sql = """
            SELECT new com.lianhua.erp.dto.report.EmployeeYearlySalaryReportDto(
                emp.id, emp.fullName,
                YEAR(e.expenseDate),
                SUM(e.amount)
            )
            FROM Expense e
            JOIN Employee emp ON e.employee.id = emp.id
            WHERE e.employee IS NOT NULL
            GROUP BY emp.id, emp.fullName, YEAR(e.expenseDate)
            ORDER BY YEAR(e.expenseDate), emp.fullName
        """;
        return entityManager.createQuery(sql, EmployeeYearlySalaryReportDto.class).getResultList();
    }

    @Override
    public List<MonthlySalaryDetailDto> getMonthlySalaryDetails(String month) {
        String sql = """
            SELECT new com.lianhua.erp.dto.report.MonthlySalaryDetailDto(
                emp.id, emp.fullName,
                CAST(e.expenseDate AS string),
                e.amount,
                e.note
            )
            FROM Expense e
            JOIN Employee emp ON e.employee.id = emp.id
            WHERE e.employee IS NOT NULL
              AND FUNCTION('DATE_FORMAT', e.expenseDate, '%Y-%m') = :month
            ORDER BY e.expenseDate, emp.fullName
        """;
        return entityManager.createQuery(sql, MonthlySalaryDetailDto.class)
                .setParameter("month", month) // 例如 "2025-10"
                .getResultList();
    }

    @Override
    public List<OperatingCostReportDto> getMonthlyOperatingCosts() {
        String sql = """
        SELECT new com.lianhua.erp.dto.report.OperatingCostReportDto(
            FUNCTION('DATE_FORMAT', e.expenseDate, '%Y-%m'),
            SUM(e.amount)
        )
        FROM Expense e
        WHERE e.employee IS NULL
        GROUP BY FUNCTION('DATE_FORMAT', e.expenseDate, '%Y-%m')
        ORDER BY FUNCTION('DATE_FORMAT', e.expenseDate, '%Y-%m')
    """;
        return entityManager.createQuery(sql, OperatingCostReportDto.class).getResultList();
    }

    @Override
    public List<OperatingCostCategoryDto> getOperatingCostByCategory() {
        String sql = """
        SELECT new com.lianhua.erp.dto.report.OperatingCostCategoryDto(
            e.category,
            SUM(e.amount)
        )
        FROM Expense e
        WHERE e.employee IS NULL
        GROUP BY e.category
        ORDER BY SUM(e.amount) DESC
    """;
        return entityManager.createQuery(sql, OperatingCostCategoryDto.class).getResultList();
    }

    @Override
    public List<OperatingCostMonthlyCategoryDto> getMonthlyCategoryOperatingCosts() {
        String sql = """
        SELECT new com.lianhua.erp.dto.report.OperatingCostMonthlyCategoryDto(
            FUNCTION('DATE_FORMAT', e.expenseDate, '%Y-%m'),
            e.category,
            SUM(e.amount)
        )
        FROM Expense e
        WHERE e.employee IS NULL
        GROUP BY FUNCTION('DATE_FORMAT', e.expenseDate, '%Y-%m'), e.category
        ORDER BY FUNCTION('DATE_FORMAT', e.expenseDate, '%Y-%m'), SUM(e.amount) DESC
    """;
        return entityManager.createQuery(sql, OperatingCostMonthlyCategoryDto.class).getResultList();
    }
}
