package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.dashboard.DashboardStatsDto;
import com.lianhua.erp.dto.dashboard.TrendPointDto;
import com.lianhua.erp.dto.error.BadRequestResponse;
import com.lianhua.erp.dto.error.InternalServerErrorResponse;
import com.lianhua.erp.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "營運儀表板", description = "Dashboard Management API")
public class DashboardController {

    private final DashboardService service;

    // ================================
    // 查詢核心 KPI 統計
    // ================================
    @GetMapping("/stats")
    @Operation(
            summary = "獲取核心 KPI 統計",
            description = "整合銷售、採購、費用與財務指標。自動過濾作廢單據，並計算當前會計期間的利潤與利率。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得儀表板統計數據",
                    content = @Content(schema = @Schema(implementation = DashboardStatsDto.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto<DashboardStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getDashboardStats()));
    }

    // ================================
    // 查詢營運趨勢數據
    // ================================
    @GetMapping("/trends")
    @Operation(
            summary = "獲取營運趨勢圖數據",
            description = "取得最近指定天數（預設 30 天）的每日營收趨勢，用於前端 Recharts 渲染。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得趨勢圖數據",
                    content = @Content(schema = @Schema(implementation = TrendPointDto.class))),
            @ApiResponse(responseCode = "400", description = "天數參數格式錯誤",
                    content = @Content(schema = @Schema(implementation = BadRequestResponse.class))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤",
                    content = @Content(schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponseDto<List<TrendPointDto>>> getTrends(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(ApiResponseDto.ok(service.getSalesTrendData(days)));
    }
}