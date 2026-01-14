package com.lianhua.erp.event;

import com.lianhua.erp.domain.Purchase;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PurchaseEvent extends ApplicationEvent {
    private final Purchase purchase;
    private final String action; // e.g., "ITEM_ADDED", "VOIDED"

    public PurchaseEvent(Object source, Purchase purchase, String action) {
        super(source);
        this.purchase = purchase;
        this.action = action;
    }
}
