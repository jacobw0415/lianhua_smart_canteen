package com.lianhua.erp.repository;

import com.lianhua.erp.domain.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {
}
