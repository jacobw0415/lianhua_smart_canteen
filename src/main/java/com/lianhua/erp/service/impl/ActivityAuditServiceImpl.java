package com.lianhua.erp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lianhua.erp.config.AuditProperties;
import com.lianhua.erp.domain.ActivityAuditLog;
import com.lianhua.erp.dto.audit.ActivityAuditLogResponseDto;
import com.lianhua.erp.dto.audit.ActivityAuditRecordRequest;
import com.lianhua.erp.dto.audit.ActivityAuditLogSearchRequest;
import com.lianhua.erp.service.impl.spec.ActivityAuditLogSpecifications;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.export.ExportFilenameUtils;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import com.lianhua.erp.export.TabularExporter;
import com.lianhua.erp.repository.ActivityAuditLogRepository;
import com.lianhua.erp.service.ActivityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityAuditServiceImpl implements ActivityAuditService {

    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final String[] ACTIVITY_AUDIT_EXPORT_HEADERS = new String[]{
            "紀錄ID",
            "發生時間(UTC)",
            "發生時間(台北)",
            "操作者ID",
            "操作者名稱",
            "動作",
            "資源類型",
            "資源ID",
            "HTTP方法",
            "請求路徑",
            "查詢字串",
            "來源IP",
            "User-Agent",
            "補充資訊"
    };

    private final ActivityAuditLogRepository repository;
    private final AuditProperties auditProperties;
    private final ObjectMapper objectMapper;

    @Value("${app.export.max-rows:50000}")
    private int maxExportRows;

    @Override
    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAsync(ActivityAuditRecordRequest request) {
        if (!auditProperties.isEnabled()) {
            return;
        }
        try {
            int maxDetails = auditProperties.getDetailsMaxChars();
            String details = request.details();
            if (maxDetails > 0 && details != null && details.length() > maxDetails) {
                details = details.substring(0, maxDetails);
            }
            ActivityAuditLog row = ActivityAuditLog.builder()
                    .occurredAt(Instant.now())
                    .operatorId(request.operatorId())
                    .operatorUsername(request.operatorUsername())
                    .action(request.action())
                    .resourceType(request.resourceType())
                    .resourceId(request.resourceId())
                    .httpMethod(request.httpMethod())
                    .requestPath(truncate(request.requestPath(), 1024))
                    .queryString(truncate(request.queryString(), 512))
                    .ipAddress(truncate(request.ipAddress(), 45))
                    .userAgent(truncate(request.userAgent(), 512))
                    .details(details)
                    .build();
            repository.save(row);
        } catch (Exception e) {
            log.warn("Failed to persist activity audit log: {}", e.getMessage());
            log.debug("Activity audit persistence failure", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityAuditLogResponseDto> search(Pageable pageable, ActivityAuditLogSearchRequest request) {
        Pageable safePageable = normalizePageable(pageable);
        try {
            var page = repository.findAll(
                    ActivityAuditLogSpecifications.bySearchRequest(request),
                    safePageable
            );

            return page.map(this::toDto);
        } catch (PropertyReferenceException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "無效排序欄位：" + ex.getPropertyName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExportPayload exportAuditLogs(
            ActivityAuditLogSearchRequest request,
            Pageable pageable,
            ExportFormat format,
            ExportScope scope
    ) {
        ActivityAuditLogSearchRequest req = request == null
                ? new ActivityAuditLogSearchRequest(null, null, null, null, null, null, null)
                : request;
        ExportFormat safeFormat = format == null ? ExportFormat.XLSX : format;
        ExportScope safeScope = scope == null ? ExportScope.ALL : scope;

        Specification<ActivityAuditLog> spec = ActivityAuditLogSpecifications.bySearchRequest(req);
        Sort safeSort = pageable != null && pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "occurredAt");

        List<ActivityAuditLog> logs;

        if (safeScope == ExportScope.ALL) {
            long total = repository.count(spec);
            if (total > maxExportRows) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "匯出筆數超過上限 (" + maxExportRows + ")，請縮小篩選條件");
            }
            logs = new ArrayList<>((int) Math.min(total, Integer.MAX_VALUE));

            int step = 1000;
            if (pageable != null && pageable.isPaged()
                    && pageable.getPageSize() > 0 && pageable.getPageSize() <= 200) {
                step = Math.max(50, pageable.getPageSize());
            }
            int pages = total == 0 ? 0 : (int) ((total + step - 1) / step);
            try {
                for (int p = 0; p < pages; p++) {
                    Page<ActivityAuditLog> page = repository.findAll(spec, PageRequest.of(p, step, safeSort));
                    logs.addAll(page.getContent());
                }
            } catch (PropertyReferenceException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "無效排序欄位：" + ex.getPropertyName());
            }
        } else {
            logs = new ArrayList<>();
            Pageable p = pageable == null || !pageable.isPaged()
                    ? PageRequest.of(0, 25, safeSort)
                    : normalizeForExport(pageable, safeSort);
            try {
                Page<ActivityAuditLog> page = repository.findAll(spec, p);
                logs.addAll(page.getContent());
            } catch (PropertyReferenceException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "無效排序欄位：" + ex.getPropertyName());
            }
        }

        List<String[]> rows = new ArrayList<>(logs.size());
        for (ActivityAuditLog e : logs) {
            rows.add(toExportRow(e));
        }

        byte[] data = switch (safeFormat) {
            case XLSX -> TabularExporter.toXlsx("活動稽核", ACTIVITY_AUDIT_EXPORT_HEADERS, rows);
            case CSV -> TabularExporter.toCsvUtf8Bom(ACTIVITY_AUDIT_EXPORT_HEADERS, rows);
        };
        String filename = ExportFilenameUtils.build("activity_audit_logs", safeFormat);
        return new ExportPayload(data, filename, safeFormat.mediaType());
    }

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable == null || !pageable.isPaged()) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "occurredAt"));
        }
        if (pageable.getPageNumber() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 不可小於 0");
        }
        if (pageable.getPageSize() <= 0 || pageable.getPageSize() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size 需介於 1 - 200 之間");
        }
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "occurredAt");
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Pageable normalizeForExport(Pageable pageable, Sort safeSort) {
        if (!pageable.isPaged()) {
            return PageRequest.of(0, 25, safeSort);
        }
        if (pageable.getPageNumber() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 不可小於 0");
        }
        int size = pageable.getPageSize();
        if (size <= 0 || size > 200) {
            size = 25;
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }

    private static String[] toExportRow(ActivityAuditLog e) {
        return new String[]{
                e.getId() != null ? String.valueOf(e.getId()) : "",
                e.getOccurredAt() != null ? e.getOccurredAt().toString() : "",
                formatTaipei(e.getOccurredAt()),
                e.getOperatorId() != null ? String.valueOf(e.getOperatorId()) : "",
                nullToEmpty(e.getOperatorUsername()),
                nullToEmpty(e.getAction()),
                nullToEmpty(e.getResourceType()),
                e.getResourceId() != null ? String.valueOf(e.getResourceId()) : "",
                nullToEmpty(e.getHttpMethod()),
                nullToEmpty(e.getRequestPath()),
                nullToEmpty(e.getQueryString()),
                nullToEmpty(e.getIpAddress()),
                nullToEmpty(e.getUserAgent()),
                truncateForExport(e.getDetails())
        };
    }

    private ActivityAuditLogResponseDto toDto(ActivityAuditLog e) {
        Integer responseStatus = null;
        Long durationMs = null;
        String details = e.getDetails();
        if (details != null && !details.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(details);
                if (node.has("httpStatus") && !node.get("httpStatus").isNull()) {
                    responseStatus = node.get("httpStatus").asInt();
                }
                if (node.has("durationMs") && !node.get("durationMs").isNull()) {
                    durationMs = node.get("durationMs").asLong();
                }
            } catch (Exception ex) {
                // details 不是固定 schema；解析失敗就維持空值，不影響清單查詢
                log.debug("Failed to parse activity audit details JSON: {}", ex.getMessage());
            }
        }
        return new ActivityAuditLogResponseDto(
                e.getId(),
                e.getOccurredAt(),
                formatTaipei(e.getOccurredAt()),
                e.getOperatorId(),
                e.getOperatorUsername(),
                e.getAction(),
                e.getResourceType(),
                e.getResourceId(),
                e.getHttpMethod(),
                responseStatus,
                durationMs,
                e.getRequestPath(),
                e.getQueryString(),
                e.getIpAddress(),
                e.getUserAgent(),
                details
        );
    }

    private static String formatTaipei(Instant instant) {
        if (instant == null) {
            return "";
        }
        return ISO_OFFSET.format(instant.atZone(TAIPEI));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncateForExport(String details) {
        if (details == null) {
            return "";
        }
        return details.length() > 4000 ? details.substring(0, 4000) + "…" : details;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen);
    }
}
