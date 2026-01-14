package com.lianhua.erp.component;

import com.lianhua.erp.event.PurchaseEvent;
import com.lianhua.erp.service.NotificationService;
import com.lianhua.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🔥 加入 Log
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Async
    @EventListener
    public void handlePurchaseEvent(PurchaseEvent event) {
        log.info("🔔 收到採購事件通知: Action={}, PurchaseNo={}", event.getAction(), event.getPurchase().getPurchaseNo());

        Map<String, Object> payload = new HashMap<>();
        payload.put("purchaseNo", event.getPurchase().getPurchaseNo());

        // 預設發送給 ID 為 1 的測試使用者
        List<Long> testReceiverIds = List.of(1L);

        // 🔥 1. 新增：處理「新增進貨單」通知
        if ("PURCHASE_CREATED".equals(event.getAction())) {
            log.info("➡️ 處理 [新增進貨單] 通知...");
            notificationService.send("PURCHASE_CREATED_ALERT", "purchases",
                    event.getPurchase().getId(), payload, testReceiverIds);
        }

        // 2. 處理「作廢」通知
        if ("VOIDED".equals(event.getAction())) {
            log.info("➡️ 處理 [作廢] 通知...");
            notificationService.send("PURCHASE_VOIDED", "purchases",
                    event.getPurchase().getId(), payload, testReceiverIds);
        }
        
    }
}