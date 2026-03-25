package com.lianhua.erp.service;

import com.lianhua.erp.dto.audit.ActivityAuditLogResponseDto;
import com.lianhua.erp.dto.audit.ActivityAuditRecordRequest;
import com.lianhua.erp.dto.audit.ActivityAuditLogSearchRequest;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ActivityAuditService {

    /**
     * 非同步寫入稽核紀錄（失敗僅記錄 log，不影響主流程）。
     */
    void recordAsync(ActivityAuditRecordRequest request);

    Page<ActivityAuditLogResponseDto> search(Pageable pageable, ActivityAuditLogSearchRequest request);

    /**
     * 匯出稽核紀錄；篩選與列表相同，並支援 scope=all（全選）／page（當前頁），與其他模組匯出規格一致。
     */
    ExportPayload exportAuditLogs(
            ActivityAuditLogSearchRequest request,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    );
}
