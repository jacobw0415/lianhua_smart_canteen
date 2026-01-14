package com.lianhua.erp.dto.notification;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class NotificationRequestDto {

    private String templateCode;   // e.g., "PURCHASE_VOIDED"
    private String targetType;     // e.g., "purchases"
    private Long targetId;         // 單據 ID

    // 動態內容，例如 {"no": "PO-123", "amount": 5000}
    private Map<String, Object> payload;

    private Integer priority;      // 1-3
    private List<Long> receiverIds; // 接收者 User ID 清單
}