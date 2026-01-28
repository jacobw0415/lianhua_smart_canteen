package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<Order, Long> {

    /* =========================================================
     * 1. 營運概況 (第一排字卡)
     * ========================================================= */

    /** 今日營收：加總今日所有零售銷售額 */
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM sales WHERE sale_date = :today", nativeQuery = true)
    BigDecimal getTodaySalesTotal(@Param("today") LocalDate today);

    /** 本月採購：加總本會計期間內非作廢的採購金額 */
    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM purchases " +
            "WHERE accounting_period = :period AND record_status = 'ACTIVE'", nativeQuery = true)
    BigDecimal getMonthPurchaseTotal(@Param("period") String period);

    /** 本月費用：加總本會計期間內非作廢的費用支出 */
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM expenses " +
            "WHERE accounting_period = :period AND status = 'ACTIVE'", nativeQuery = true)
    BigDecimal getMonthExpenseTotal(@Param("period") String period);


    /* =========================================================
     * 2. 財務指標 (第二排字卡)
     * ========================================================= */

    /** 本月銷售總額：加總本會計期間內零售銷售額 */
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM sales WHERE accounting_period = :period", nativeQuery = true)
    BigDecimal getMonthSalesTotal(@Param("period") String period);

    /** 應收帳款 (AR)：加總所有非作廢訂單的未收餘額（總額 - 已收金額） */
    @Query(value = "SELECT COALESCE(SUM(o.total_amount - (SELECT COALESCE(SUM(r.amount), 0) FROM receipts r WHERE r.order_id = o.id AND r.status = 'ACTIVE')), 0) " +
            "FROM orders o WHERE o.record_status = 'ACTIVE' AND o.payment_status != 'PAID'", nativeQuery = true)
    BigDecimal getAccountsReceivableTotal();

    /** 應付帳款 (AP)：加總所有非作廢採購單的未付餘額 */
    @Query(value = "SELECT COALESCE(SUM(balance), 0) FROM purchases WHERE record_status = 'ACTIVE' AND status != 'PAID'", nativeQuery = true)
    BigDecimal getAccountsPayableTotal();


    /* =========================================================
     * 3. 業務概況 (第三排字卡)
     * ========================================================= */

    /** 合作供應商：統計目前活躍的供應商總數 */
    @Query(value = "SELECT COUNT(*) FROM suppliers WHERE active = 1", nativeQuery = true)
    long countActiveSuppliers();

    /** 累計客戶：統計所有訂單商家總數 */
    @Query(value = "SELECT COUNT(*) FROM order_customers", nativeQuery = true)
    long countTotalCustomers();

    /** 上架商品：統計目前標記為有效（Active）的商品數 */
    @Query(value = "SELECT COUNT(*) FROM products WHERE active = 1", nativeQuery = true)
    long countActiveProducts();

    /** 未結案訂單：統計狀態非「DELIVERED」且非「CANCELLED」的活躍訂單數 */
    @Query(value = "SELECT COUNT(*) FROM orders " +
            "WHERE order_status NOT IN ('DELIVERED', 'CANCELLED') AND record_status = 'ACTIVE'", nativeQuery = true)
    int getPendingOrderCount();

    /** 獲取每日銷售趨勢數據 */
    @Query(value = "SELECT DATE(sale_date) as date, COALESCE(SUM(amount), 0) as amount " +
            "FROM sales WHERE sale_date >= :startDate AND sale_date <= :endDate " +
            "GROUP BY DATE(sale_date) ORDER BY DATE(sale_date)", nativeQuery = true)
    List<Object[]> getDailySalesTrend(@Param("startDate") LocalDate startDate);

    /* =========================================================
     * 4. 現金流量 (第四排字卡)
     * ========================================================= */

    /** 今日訂單收款：加總今日所有已入帳（ACTIVE）的訂單收款金額 */
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM receipts " +
            "WHERE received_date = :today AND status = 'ACTIVE'", nativeQuery = true)
    BigDecimal getTodayReceiptsTotal(@Param("today") LocalDate today);

    /** 今日總入金：今日現場零售額 + 今日訂單收款額 */
    @Query(value = "SELECT " +
            "(SELECT COALESCE(SUM(amount), 0) FROM sales WHERE sale_date = :today) + " +
            "(SELECT COALESCE(SUM(amount), 0) FROM receipts WHERE received_date = :today AND status = 'ACTIVE')",
            nativeQuery = true)
    BigDecimal getTodayTotalInflow(@Param("today") LocalDate today);

    /** 本月累計實收：加總本會計期間內所有零售額與訂單收款額 */
    @Query(value = "SELECT " +
            "(SELECT COALESCE(SUM(amount), 0) FROM sales WHERE accounting_period = :period) + " +
            "(SELECT COALESCE(SUM(amount), 0) FROM receipts WHERE accounting_period = :period AND status = 'ACTIVE')",
            nativeQuery = true)
    BigDecimal getMonthTotalReceived(@Param("period") String period);

    /** 即期應收 (7D)：統計未來 7 天內到期（或已過期）且尚未付清的活躍訂單餘額 */
    @Query(value = "SELECT COALESCE(SUM(o.total_amount - (SELECT COALESCE(SUM(r.amount), 0) FROM receipts r WHERE r.order_id = o.id AND r.status = 'ACTIVE')), 0) " +
            "FROM orders o WHERE o.record_status = 'ACTIVE' " +
            "AND o.payment_status != 'PAID' " +
            "AND o.delivery_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)", nativeQuery = true)
    BigDecimal getUpcomingAR();
}