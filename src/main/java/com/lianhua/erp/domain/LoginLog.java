package com.lianhua.erp.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "login_ip", length = 45)
    private String loginIp;

    @Column(name = "login_at")
    private LocalDateTime loginAt;

    @Enumerated(EnumType.STRING)
    private LoginStatus status;

    @Column(name = "user_agent")
    private String userAgent;

    public enum LoginStatus {
        SUCCESS, FAILED
    }

    @PrePersist
    public void prePersist() {
        this.loginAt = LocalDateTime.now();
    }
}