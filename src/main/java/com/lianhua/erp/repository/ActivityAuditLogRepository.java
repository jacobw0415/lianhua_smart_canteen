package com.lianhua.erp.repository;

import com.lianhua.erp.domain.ActivityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActivityAuditLogRepository extends JpaRepository<ActivityAuditLog, Long>,
        JpaSpecificationExecutor<ActivityAuditLog> {
}
