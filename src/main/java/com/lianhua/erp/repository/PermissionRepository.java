package com.lianhua.erp.repository;

import com.lianhua.erp.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 權限資料存取層
 * 用於管理 ERP 系統中最小顆粒度的功能權限
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * 透過權限名稱查找（如：purchase:view）
     * 用於初始化數據檢查或權限校驗
     */
    Optional<Permission> findByName(String name);

    /**
     * 根據模組名稱查找權限清單（如：進貨、銷售）
     * 用於前端 React-Admin 在設定角色權限時按模組分組顯示
     */
    List<Permission> findByModule(String module);

    /**
     * 檢查特定權限名稱是否存在
     */
    boolean existsByName(String name);
}