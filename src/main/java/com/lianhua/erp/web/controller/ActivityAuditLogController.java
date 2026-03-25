package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.audit.ActivityAuditLogResponseDto;
import com.lianhua.erp.dto.audit.ActivityAuditLogSearchRequest;
import com.lianhua.erp.dto.error.ForbiddenResponse;
import com.lianhua.erp.dto.error.UnauthorizedResponse;
import com.lianhua.erp.dto.export.ExportPayload;
import com.lianhua.erp.export.ExportFormat;
import com.lianhua.erp.export.ExportScope;
import com.lianhua.erp.service.ActivityAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 全系統活動稽核查詢（僅超級管理員）。
 */
@RestController
@RequestMapping("/api/admin/activity-audit-logs")
@RequiredArgsConstructor
@Tag(name = "全系統活動稽核", description = "查詢 HTTP 層自動紀錄之新增／變更／刪除／匯出等行為")
public class ActivityAuditLogController {

    private final ActivityAuditService activityAuditService;

    @GetMapping
    @Operation(
            summary = "分頁查詢活動稽核紀錄",
            description = """
                    篩選條件與匯出 GET /export 相同（operatorId、action、resourceType、from、to）。
                    支援 page / size / sort；預設依 occurredAt 降序；size 限 1–200。
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未授權", content = @Content(schema = @Schema(implementation = UnauthorizedResponse.class))),
            @ApiResponse(responseCode = "403", description = "僅超級管理員", content = @Content(schema = @Schema(implementation = ForbiddenResponse.class)))
    })
    @PageableAsQueryParam
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<Page<ActivityAuditLogResponseDto>>> list(
            @ParameterObject
            @PageableDefault(sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable,
            @ParameterObject ActivityAuditLogSearchRequest request
    ) {
        Page<ActivityAuditLogResponseDto> page = activityAuditService.search(pageable, request);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }

    @GetMapping(value = "/export", produces = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv; charset=UTF-8"
    })
    @Operation(
            summary = "匯出活動稽核紀錄",
            description = """
                    篩選條件與分頁查詢相同（operatorId、action、resourceType、from、to）。
                    - scope=page：匯出目前分頁（依 page / size / sort）
                    - scope=all（預設）：匯出全部符合條件資料（受 app.export.max-rows 上限）
                    - format：xlsx（預設）或 csv
                    """
    )
    @PageableAsQueryParam
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功下載檔案"),
            @ApiResponse(responseCode = "400", description = "筆數超過上限或參數錯誤"),
            @ApiResponse(responseCode = "401", description = "未授權", content = @Content(schema = @Schema(implementation = UnauthorizedResponse.class))),
            @ApiResponse(responseCode = "403", description = "僅超級管理員", content = @Content(schema = @Schema(implementation = ForbiddenResponse.class)))
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> export(
            @ParameterObject ActivityAuditLogSearchRequest request,
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String scope
    ) {
        String resolvedScope = (scope == null || scope.isBlank()) ? "all" : scope;
        ExportPayload payload = activityAuditService.exportAuditLogs(
                request,
                pageable,
                ExportFormat.fromQueryParam(format),
                ExportScope.fromQueryParam(resolvedScope)
        );
        ContentDisposition disposition = ContentDisposition.builder("attachment")
                .filename(payload.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(payload.mediaType()))
                .body(payload.data());
    }
}
