package com.lianhua.erp.event;

import com.lianhua.erp.domain.Purchase;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.Map;
import java.util.HashMap;

@Getter
public class PurchaseEvent extends ApplicationEvent {
    private final Purchase purchase;
    private final String action;
    // 新增：用於攜帶動態資訊，如作廢原因、操作者姓名、金額摘要等
    private final Map<String, Object> payload;

    public PurchaseEvent(Object source, Purchase purchase, String action) {
        super(source);
        this.purchase = purchase;
        this.action = action;
        this.payload = new HashMap<>();
    }

    // 支援攜帶額外資料的建構子
    public PurchaseEvent(Object source, Purchase purchase, String action, Map<String, Object> payload) {
        super(source);
        this.purchase = purchase;
        this.action = action;
        this.payload = payload != null ? payload : new HashMap<>();
    }
}