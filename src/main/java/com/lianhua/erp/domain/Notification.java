// src/main/java/com/lianhua/erp/domain/Notification.java
package com.lianhua.erp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_code")
    private String templateCode;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(columnDefinition = "json")
    private String payload;

    private Integer priority = 1;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "message") // 確保欄位對應正確
    private String message;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}