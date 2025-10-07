package com.lianhua.erp.domin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "user_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {
    
    @EmbeddedId
    private UserRoleId id = new UserRoleId();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties("userRoles")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    @JsonIgnoreProperties("userRoles")
    private Role role;
    
    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
        this.id = new UserRoleId(
                user != null ? user.getId() : null,
                role != null ? role.getId() : null
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRole that)) return false;
        return id != null && id.equals(that.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
