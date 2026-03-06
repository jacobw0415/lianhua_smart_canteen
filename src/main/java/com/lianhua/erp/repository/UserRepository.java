package com.lianhua.erp.repository;

import com.lianhua.erp.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /** 透過帳號查找使用者（登入核心邏輯） */
    Optional<User> findByUsername(String username);

    //  /** 透過帳號查找信箱（登入核心邏輯） */
    Optional<User> findByEmail(String email);

    /** 取得單一使用者並抓取角色
     * 註：若 Entity 已設為 EAGER，此方法可簡化，但使用 JOIN FETCH 可確保在不同情境下的效能優化
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.roles " +
            "WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    /** 根據角色名稱查找使用者 ID 清單
     * 改為直接透過 u.roles 導航
     */
    @Query("SELECT DISTINCT u.id FROM User u " +
            "JOIN u.roles r " +
            "WHERE r.name = :roleName")
    List<Long> findIdsByRoleName(@Param("roleName") String roleName);

    /** 檢查帳號是否存在 */
    boolean existsByUsername(String username);

    /** 檢查 Email 是否已被使用 */
    boolean existsByEmail(String email);

    /** 統計「啟用中且具 ROLE_ADMIN」的使用者數量（用於保護最後一位管理員） */
    @Query("SELECT COUNT(DISTINCT u.id) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN' AND u.enabled = true")
    long countEnabledUsersWithRoleAdmin();

    /** 統計「啟用中且具 ROLE_ADMIN」的使用者數量，排除指定使用者 id（用於更新時檢查：若將該使用者改為非管理員或停用，剩餘數量是否為 0） */
    @Query("SELECT COUNT(DISTINCT u.id) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN' AND u.enabled = true AND u.id <> :excludeUserId")
    long countEnabledUsersWithRoleAdminExcluding(@Param("excludeUserId") Long excludeUserId);

    /** 統計「啟用中且具 ROLE_ADMIN 或 ROLE_SUPER_ADMIN」的使用者數量（用於保護至少保留一位管理員） */
    @Query("SELECT COUNT(DISTINCT u.id) FROM User u JOIN u.roles r WHERE r.name IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN') AND u.enabled = true")
    long countEnabledUsersWithAnyAdminRole();

    /** 同上，排除指定使用者 id */
    @Query("SELECT COUNT(DISTINCT u.id) FROM User u JOIN u.roles r WHERE r.name IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN') AND u.enabled = true AND u.id <> :excludeUserId")
    long countEnabledUsersWithAnyAdminRoleExcluding(@Param("excludeUserId") Long excludeUserId);

    /** 取得所有「啟用中且具 ROLE_ADMIN 或 ROLE_SUPER_ADMIN」的使用者 ID，供系統通知發送給所有管理員 */
    @Query("SELECT DISTINCT u.id FROM User u JOIN u.roles r WHERE r.name IN ('ROLE_ADMIN', 'ROLE_SUPER_ADMIN') AND u.enabled = true")
    List<Long> findEnabledAdminIds();
}