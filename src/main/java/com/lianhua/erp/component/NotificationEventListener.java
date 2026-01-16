package com.lianhua.erp.component;

import com.lianhua.erp.event.PurchaseEvent;
import com.lianhua.erp.event.ReceiptEvent; // 🚀 新增匯入
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

        List<Long> receiverIds = List.of(1L);

        switch (action) {
            case "PURCHASE_VOIDED":
                notificationService.send("PURCHASE_VOID_ALERT", "purchases",
                        event.getPurchase().getId(), finalPayload, receiverIds);
                break;
            // 未來可擴充 PURCHASE_CREATED 等
        }
    }

    /**
     * 2. ✨ 新增：監聽收款相關事件 (收款單)
     */
    @Async
    @EventListener
    public void handleReceiptEvent(ReceiptEvent event) {
        String action = event.getAction();
        // 這裡透過 receipt.getOrder() 取得單號
        String orderNo = event.getReceipt().getOrder().getOrderNo();

        log.info("🔔 [事件監聽] 收到收款事件: Action={}, OrderNo={}", action, orderNo);

        // 1. 構建 Payload
        Map<String, Object> finalPayload = new HashMap<>();
        finalPayload.put("no", orderNo);
        finalPayload.put("amount", event.getReceipt().getAmount());

        // 2. 併入 Service 傳來的 reason
        if (event.getPayload() != null) {
            finalPayload.putAll(event.getPayload());
        }

        List<Long> receiverIds = List.of(1L);

        // 3. 根據動作分發
        switch (action) {
            case "RECEIPT_VOIDED":
                log.info("🚫 執行 [收款單作廢] 通知發送，原因: {}", finalPayload.getOrDefault("reason", "無"));
                // 這裡的 "RECEIPT_VOID_ALERT" 需對應 NotificationServiceImpl 的 renderText
                notificationService.send(
                        "RECEIPT_VOID_ALERT",
                        "orders",
                        event.getReceipt().getOrder().getId(),
                        finalPayload,
                        receiverIds
                );
                break;

            case "RECEIPT_CREATED":
                log.info("✨ 執行 [新增收款] 通知發送");
                notificationService.send(
                        "RECEIPT_CREATED_ALERT",
                        "orders",
                        event.getReceipt().getOrder().getId(),
                        finalPayload,
                        receiverIds
                );
                break;
        }
    }
}