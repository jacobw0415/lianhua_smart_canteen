package com.lianhua.erp.service.impl;

import com.lianhua.erp.dto.report.OrderReportDto;
import com.lianhua.erp.dto.report.ProductSalesReportDto;
import com.lianhua.erp.service.OrderReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderReportServiceImpl implements OrderReportService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<OrderReportDto> getAnnualSummary(int year) {
        String sql = """
            SELECT new com.lianhua.erp.dto.OrderReportDto(
                oc.id, oc.name, CAST(YEAR(o.orderDate) AS string),
                null, COUNT(DISTINCT o.id), SUM(o.totalAmount)
            )
            FROM Order o
            JOIN o.customer oc
            WHERE YEAR(o.orderDate) = :year
            GROUP BY oc.id, oc.name, YEAR(o.orderDate)
            ORDER BY SUM(o.totalAmount) DESC
        """;
        return entityManager.createQuery(sql, OrderReportDto.class)
                .setParameter("year", year)
                .getResultList();
    }

    @Override
    public List<OrderReportDto> getMonthlySummary(int year) {
        String sql = """
            SELECT new com.lianhua.erp.dto.OrderReportDto(
                oc.id, oc.name, FUNCTION('DATE_FORMAT', o.orderDate, '%Y-%m'),
                null, COUNT(DISTINCT o.id), SUM(o.totalAmount)
            )
            FROM Order o
            JOIN o.customer oc
            WHERE YEAR(o.orderDate) = :year
            GROUP BY oc.id, oc.name, FUNCTION('DATE_FORMAT', o.orderDate, '%Y-%m')
            ORDER BY oc.name, FUNCTION('DATE_FORMAT', o.orderDate, '%Y-%m')
        """;
        return entityManager.createQuery(sql, OrderReportDto.class)
                .setParameter("year", year)
                .getResultList();
    }

    @Override
    public List<OrderReportDto> getOutstandingOrders(int months) {
        String sql = """
            SELECT new com.lianhua.erp.dto.OrderReportDto(
                oc.id, oc.name, null, oc.billingCycle,
                COUNT(DISTINCT o.id), SUM(o.totalAmount)
            )
            FROM Order o
            JOIN o.customer oc
            WHERE o.status = 'PENDING'
              AND o.orderDate >= CURRENT_DATE - :months
            GROUP BY oc.id, oc.name, oc.billingCycle
            ORDER BY SUM(o.totalAmount) DESC
        """;
        return entityManager.createQuery(sql, OrderReportDto.class)
                .setParameter("months", months)
                .getResultList();
    }

    @Override
    public List<ProductSalesReportDto> getTopSellingProducts(int year) {
        String sql = """
            SELECT new com.lianhua.erp.dto.ProductSalesReportDto(
                p.id, p.name, SUM(oi.qty), SUM(oi.subtotal)
            )
            FROM OrderItem oi
            JOIN oi.product p
            JOIN oi.order o
            WHERE YEAR(o.orderDate) = :year
            GROUP BY p.id, p.name
            ORDER BY SUM(oi.subtotal) DESC
        """;
        return entityManager.createQuery(sql, ProductSalesReportDto.class)
                .setParameter("year", year)
                .getResultList();
    }
}
