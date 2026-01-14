package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.NotFoundResponse;
import com.lianhua.erp.dto.notification.NotificationCountDto;
import com.lianhua.erp.dto.notification.NotificationResponseDto;
import com.lianhua.erp.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知中心 API
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "通知中心", description = "處理使用者通知、未讀計數與已讀狀態 API")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "http://localhost:5173",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.OPTIONS},
        allowCredentials = "true"
)
public class NotificationController {

    private final NotificationService notificationService;

    // TODO: 串接 SecurityUtils 取得當前使用者 ID，目前暫代為 mockUserId 或從 Header 傳入
    private Long getCurrentUserId() {
        // 實際開發時請替換為：return SecurityUtils.getCurrentUserId();
        return 1L;
    }

    // ============================================================
    // 🔔 取得未讀清單 (用於小鈴鐺)
    // ============================================================
    @Operation(
            summary = "取得當前使用者的未讀通知列表",
            description = "回傳經過渲染後的標題與內容，適用於頂欄小鈴鐺快速預覽。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功取得未讀通知"),
            @ApiResponse(responseCode = "500", description = "伺服器錯誤")
    })
    @GetMapping("/unread")
    public ResponseEntity<ApiResponseDto<List<NotificationResponseDto>>> getUnreadNotifications() {
        List<NotificationResponseDto> unread = notificationService.getUnreadList(getCurrentUserId());
        return ResponseEntity.ok(ApiResponseDto.ok(unread));
    }

    // ============================================================
    // 🔢 取得未讀總數 (用於 Badge)
    // ============================================================
    @Operation(summary = "取得未讀通知總數", description = "用於小鈴鐺圖標上的數字標記 (Badge)。")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponseDto<NotificationCountDto>> getUnreadCount() {
        long count = notificationService.getUnreadCount(getCurrentUserId());
        return ResponseEntity.ok(ApiResponseDto.ok(new NotificationCountDto(count)));
    }

    // ============================================================
    // ✅ 標記已讀
    // ============================================================
    @Operation(summary = "標記特定通知為已讀")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功標記為已讀"),
            @ApiResponse(responseCode = "404",
                    description = "找不到該通知記錄",
                    content = @Content(schema = @Schema(implementation = NotFoundResponse.class)))
    })
    @PatchMapping("/{userNotificationId}/read")
    public ResponseEntity<ApiResponseDto<Void>> markAsRead(@PathVariable Long userNotificationId) {
        notificationService.markAsRead(userNotificationId);
        return ResponseEntity.ok(ApiResponseDto.ok(null));
    }

    // ============================================================
    // 📜 取得所有通知 (通知中心分頁頁面)
    // ============================================================
    @Operation(
            summary = "取得使用者所有通知歷史",
            description = "用於獨立的通知管理頁面，包含已讀與未讀。未來可擴充 Pageable 支持。"
    )
    @GetMapping("/all")
    public ResponseEntity<ApiResponseDto<List<NotificationResponseDto>>> getAllNotifications() {
        // 這裡可以調用 service.getAllByUserId(userId)
        // 目前暫用 unreadList 邏輯示意
        List<NotificationResponseDto> all = notificationService.getUnreadList(getCurrentUserId());
        return ResponseEntity.ok(ApiResponseDto.ok(all));
    }
}