package com.lianhua.erp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 權限實體
 * 對應資料表：permissions
 * 顆粒度：按鈕級別或 API 級別，例如 "purchase:void"
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String name; // 權限識別碼，例如：purchase:view, purchase:void, order:create

    @Column(length = 100)
    private String description; // 中文描述，例如：作廢採購單

    @Column(length = 50)
    private String module; // 所屬模組，例如：進貨、銷售、財務

    // ===============================
    // 🔹 關聯設定：Permission ↔ Role (多對多反向)
    // ===============================

    @JsonIgnore // 避免序列化時產生循環參照
    @ManyToMany(mappedBy = "permissions")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // ===============================
    // 🔹 輔助方法
    // ===============================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", module='" + module + '\'' +
                '}';
    }
}