package com.lianhua.erp.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token; // 🌿 存儲 UUID 字串

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user; // 🌿 關聯到使用者表

    @Column(nullable = false)
    private LocalDateTime expiryDate; // 🌿 設定過期時間 (如 15 分鐘)

    /** 檢查權杖是否已過期 */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}
