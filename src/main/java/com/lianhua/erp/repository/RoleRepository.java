package com.lianhua.erp.repository;

import com.lianhua.erp.domin.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 透過角色名稱查找角色
     * 範例：findByName("ADMIN") 或 findByName("USER")
     */
    Optional<Role> findByName(String name);
}
