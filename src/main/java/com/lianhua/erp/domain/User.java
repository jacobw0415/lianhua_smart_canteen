package com.lianhua.erp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @JsonIgnore // 安全考量：序列化 JSON 時隱藏密碼
    @Column(nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Column(unique = true)
    private String email; // 🌿 新增：對應加強版 SQL

    @Column(name = "employee_id", unique = true)
    private Long employee_id; // 🌿 新增：對應員工關聯 (可改為 @OneToOne 關聯 Employee 實體)

    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt; // 🌿 新增：紀錄登入時間

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 512)
    private String mfaSecret; // TOTP 密鑰，建議以 AES 加密儲存

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===============================
    // 🔹 簡化關聯：直接多對多 Roles
    // ===============================

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER) // 登入時通常需要立即知道權限，改用 EAGER
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    // ===============================
    // 🔹 輔助方法
    // ===============================

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}