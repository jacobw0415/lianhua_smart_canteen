package com.lianhua.erp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String name; // 例如: ROLE_ADMIN, ROLE_MANAGER

    @Column(length = 100)
    private String description; // 🌿 新增：例如「系統管理員」、「倉管人員」

    // ===============================
    // 🔹 關聯設定：Role ↔ Permission (多對多)
    // ===============================

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER) // 權限通常隨角色一同載入
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    // ===============================
    // 🔹 關聯設定：Role ↔ User (多對多反向)
    // ===============================

    @JsonIgnore // 避免循環參照
    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private Set<User> users = new HashSet<>();

    // ===============================
    // 🔹 輔助方法
    // ===============================

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return id != null && id.equals(role.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", permissionsCount=" + (permissions != null ? permissions.size() : 0) +
                '}';
    }
}