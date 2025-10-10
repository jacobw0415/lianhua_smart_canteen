package com.lianhua.erp.domin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 使用者實體
 * 對應資料表：users
 * 關聯：一對多 -> user_roles
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    
    // ===============================
    // 🔹 欄位定義
    // ===============================
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(nullable = false, unique = true, length = 60)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    private String fullName;
    
    @Builder.Default
    private Boolean enabled = true;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // ===============================
    // 🔹 關聯設定：User ↔ UserRole
    // ===============================
    
    @Builder.Default
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnoreProperties({"user", "hibernateLazyInitializer", "handler"})
    private Set<UserRole> userRoles = new HashSet<>();
    
    // ===============================
    // 🔹 輔助方法：維護雙向關聯
    // ===============================
    
    public void addRole(UserRole userRole) {
        if (userRole == null) return;
        if (userRoles == null) {
            userRoles = new HashSet<>();
        }
        userRoles.add(userRole);
        userRole.setUser(this);
    }
    
    public void removeRole(UserRole userRole) {
        if (userRole == null) return;
        if (userRoles != null) {
            userRoles.remove(userRole);
        }
        userRole.setUser(null);
    }
    
    // ===============================
    // 🔹 可選：避免 toString() 循環參照
    // ===============================
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", enabled=" + enabled +
                ", roles=" + (userRoles != null ? userRoles.size() : 0) +
                '}';
    }
}
