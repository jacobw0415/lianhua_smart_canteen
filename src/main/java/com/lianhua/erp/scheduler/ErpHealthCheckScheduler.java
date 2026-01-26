package com.lianhua.erp.scheduler;

import com.lianhua.erp.repository.ExpenseRepository; // 記得新增此 Repository 注入
import com.lianhua.erp.repository.SalesRepository;
import com.lianhua.erp.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Component
public class ErpHealthCheckScheduler {

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private ExpenseRepository expenseRepository; // 注入費用 Repository 用於第 5 項

    @Autowired
    private NotificationService notificationService;

    /**
     * 統一執行點：每天晚上 10:00 (22:00) 執行所有健康檢查
     */
//    @Scheduled(cron = "0 * * * * *")
    @Scheduled(cron = "0 0 22 * * *")
    public void runDailyHealthCheck() {
        LocalDate today = LocalDate.now();
        List<Long> adminIds = Collections.singletonList(1L); // 接收者設定

        // 執行第 4 項：今日銷售檢查
        checkDailySales(today, adminIds);

        // 執行第 5 項：本月費用檢查
        checkMonthlyExpenses(today, adminIds);

        checkDailyExpenses(today, adminIds);

        // 未來 6~9 項也可以依此類推加在這裡
    }

    /**
     * 項目 4：今日銷售紀錄檢查邏輯
     */
    private void checkDailySales(LocalDate today, List<Long> adminIds) {
        boolean hasSales = salesRepository.existsBySaleDate(today);
        if (!hasSales) {
            notificationService.sendSystemAlert(
                    "今日 (" + today + ") 尚未記錄任何銷售資料，請確認是否漏填。",
                    "WARNING",
                    adminIds
            );
        }
    }

    private void checkDailyExpenses(LocalDate today, List<Long> adminIds) {
        boolean hasExpenses = expenseRepository.existsByExpenseDate(today);
        if (!hasExpenses) {
            notificationService.sendSystemAlert(
                    "今日 (" + today + ") 尚未記錄任何支出資料，請確認是否漏填。",
                    "WARNING",
                    adminIds
            );
        }
    }


    /**
     * 項目 5：本月費用支出檢查邏輯
     */
    private void checkMonthlyExpenses(LocalDate today, List<Long> adminIds) {
        // 獲取本月第一天
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        // 檢查從本月 1 號到今日是否有任何費用紀錄
        boolean hasExpenses = expenseRepository.existsByExpenseDateBetween(firstDayOfMonth, today);

        if (!hasExpenses) {
            notificationService.sendSystemAlert(
                    "本月 (" + today.getMonthValue() + "月) 截至今日尚未輸入任何費用支出，請確認是否漏填。",
                    "WARNING",
                    adminIds
            );
        }
    }
}