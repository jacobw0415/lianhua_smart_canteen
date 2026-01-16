package com.lianhua.erp.event;

import com.lianhua.erp.domain.Receipt;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.Map;
import java.util.HashMap;

@Getter
public class ReceiptEvent extends ApplicationEvent {
    private final Receipt receipt;
    private final String action; // 例如 "RECEIPT_VOIDED"
    private final Map<String, Object> payload;

    public ReceiptEvent(Object source, Receipt receipt, String action, Map<String, Object> payload) {
        super(source);
        this.receipt = receipt;
        this.action = action;
        this.payload = payload != null ? payload : new HashMap<>();
    }
}
