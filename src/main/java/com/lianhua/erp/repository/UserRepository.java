package com.lianhua.erp.repository;

import com.lianhua.erp.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.userRoles ur " +
            "LEFT JOIN FETCH ur.role " +
            "WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @Query("select distinct u.id from User u " +
            "join u.userRoles ur " +
            "join ur.role r " +
            "where r.name = :roleName")
    List<Long> findIdsByRoleName(@Param("roleName") String roleName);
    
}
