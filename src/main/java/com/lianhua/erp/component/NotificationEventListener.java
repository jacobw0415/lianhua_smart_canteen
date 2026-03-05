package com.lianhua.erp.component;

import com.lianhua.erp.event.ExpenseEvent;
import com.lianhua.erp.event.PurchaseEvent;
import com.lianhua.erp.event.ReceiptEvent;
import com.lianhua.erp.repository.UserRepository;
import com.lianhua.erp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * 1. 監聽採購相關事件 (進貨單)
     */
    @Async
    @EventListener
    public void handlePurchaseEvent(PurchaseEvent event) {
        String action = event.getAction();
        log.info("🔔 [事件監聽] 收到採購事件: Action={}, PurchaseNo={}", action, event.getPurchase().getPurchaseNo());

        Map<String, Object> finalPayload = new HashMap<>();
        finalPayload.put("no", event.getPurchase().getPurchaseNo());
        finalPayload.put("amount", event.getPurchase().getTotalAmount());

        if (event.getPayload() != null) {
            finalPayload.putAll(event.getPayload());
        }

        List<Long> receiverIds = userRepository.findEnabledAdminIds();

        if ("PURCHASE_VOIDED".equals(action) && !receiverIds.isEmpty()) {
            notificationService.send("PURCHASE_VOID_ALERT", "purchases",
                    event.getPurchase().getId(), finalPayload, receiverIds);
        }
    }

    /**
     * 2. 監聽收款相關事件 (收款單)
     */
    @Async
    @EventListener
    public void handleReceiptEvent(ReceiptEvent event) {
        String action = event.getAction();
        String orderNo = event.getReceipt().getOrder().getOrderNo();

        log.info("🔔 [事件監聽] 收到收款事件: Action={}, OrderNo={}", action, orderNo);

        Map<String, Object> finalPayload = new HashMap<>();
        finalPayload.put("no", orderNo);
        finalPayload.put("amount", event.getReceipt().getAmount());

        if (event.getPayload() != null) {
            finalPayload.putAll(event.getPayload());
        }

        List<Long> receiverIds = userRepository.findEnabledAdminIds();

        if ("RECEIPT_VOIDED".equals(action) && !receiverIds.isEmpty()) {
            log.info("🚫 執行 [收款單作廢] 通知發送，原因: {}", finalPayload.getOrDefault("reason", "無"));
            notificationService.send(
                    "RECEIPT_VOID_ALERT",
                    "orders",
                    event.getReceipt().getOrder().getId(),
                    finalPayload,
                    receiverIds
            );
        }
    }

    /**
     * 3. ✨ 新增：監聽費用支出相關事件 (Expense)
     */
    @Async
    @EventListener
    public void handleExpenseEvent(ExpenseEvent event) {
        String action = event.getAction();
        // 取得分類名稱以利辨識 (例如：EX-003 網路費)
        String categoryName = event.getExpense().getCategory() != null ?
                event.getExpense().getCategory().getName() : "未知分類";

        log.info("🔔 [事件監聽] 收到支出事件: Action={}, ExpenseId={}", action, event.getExpense().getId());

        // 1. 構建 Payload
        Map<String, Object> finalPayload = new HashMap<>();

        // 🚀 對應您要求的格式：EX-003 (類別)
        // 注意：這裡的 "no" key 要與 Service 層發送時一致
        if (event.getPayload() != null && event.getPayload().containsKey("no")) {
            finalPayload.put("no", event.getPayload().get("no"));
        } else {
            String formattedId = String.format("EX-%03d", event.getExpense().getId());
            finalPayload.put("no", formattedId + " (" + categoryName + ")");
        }

        finalPayload.put("amount", event.getExpense().getAmount());

        // 2. 併入作廢原因
        if (event.getPayload() != null) {
            finalPayload.putAll(event.getPayload());
        }

        List<Long> receiverIds = userRepository.findEnabledAdminIds();

        // 3. 處理支出作廢通知
        if ("EXPENSE_VOIDED".equals(action) && !receiverIds.isEmpty()) {
            log.info("🚫 執行 [支出作廢] 通知發送");
            notificationService.send(
                    "EXPENSE_VOID_ALERT",
                    "expenses", // 對應資料表
                    event.getExpense().getId(),
                    finalPayload,
                    receiverIds
            );
        }
    }
}