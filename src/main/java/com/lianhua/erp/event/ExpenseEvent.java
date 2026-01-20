package com.lianhua.erp.event;

import com.lianhua.erp.domain.Expense;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.Map;
import java.util.HashMap;

@Getter
public class ExpenseEvent extends ApplicationEvent {
    private final Expense expense;
    private final String action; // e.g., "EXPENSE_VOIDED"
    private final Map<String, Object> payload;

    public ExpenseEvent(Object source, Expense expense, String action, Map<String, Object> payload) {
        super(source);
        this.expense = expense;
        this.action = action;
        this.payload = payload != null ? payload : new HashMap<>();
    }
}