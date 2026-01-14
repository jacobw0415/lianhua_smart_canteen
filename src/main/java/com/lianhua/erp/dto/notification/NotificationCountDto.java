package com.lianhua.erp.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationCountDto {
    private long unreadCount;
}