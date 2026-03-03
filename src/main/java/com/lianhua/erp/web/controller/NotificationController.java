package com.lianhua.erp.web.controller;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.notification.NotificationCountDto;
import com.lianhua.erp.dto.notification.NotificationResponseDto;
import com.lianhua.erp.security.SecurityUtils;
import com.lianhua.erp.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
        exposedHeaders = "X-Total-Count", // 重要：允許前端讀取自定義 Header
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.OPTIONS},
        allowCredentials = "true"
)
public class NotificationController {

    private final NotificationService notificationService;

    // ============================================================
    // 📜 取得所有通知 (對接 React-Admin NotificationList)
    // ============================================================
    @Operation(
            summary = "取得使用者所有通知歷史 (分頁)",
            description = "結構與客戶管理模組對齊，回傳包含 content 與分頁資訊的 Page 物件。"
    )
    @GetMapping
    @PreAuthorize("hasAuthority('notification:view')")
    public ResponseEntity<ApiResponseDto<Page<NotificationResponseDto>>> getAllNotifications(
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (currentUserId == null || currentUserId <= 0L) {
            return ResponseEntity.status(401)
                    .body(ApiResponseDto.error(401, "請先登入"));
        }
        Page<NotificationResponseDto> page = notificationService.getNotificationsPage(currentUserId, pageable);

        // 2. 直接回傳 ResponseEntity，ApiResponseDto 會包含 page 資訊 (含有 content 陣列)
        // 這樣前端 dataProvider 的 payload?.content 就能抓到資料
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }

    // ============================================================
    // 🔔 取得未讀清單 (用於小鈴鐺)
    // ============================================================
    @GetMapping("/unread")
    @PreAuthorize("hasAuthority('notification:view')")
    public ResponseEntity<ApiResponseDto<List<NotificationResponseDto>>> getUnreadNotifications() {
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (currentUserId == null || currentUserId <= 0L) {
            return ResponseEntity.status(401)
                    .body(ApiResponseDto.error(401, "請先登入"));
        }
        List<NotificationResponseDto> unread = notificationService.getUnreadList(currentUserId);
        return ResponseEntity.ok(ApiResponseDto.ok(unread));
    }

    // ============================================================
    // 🔢 取得未讀總數 (用於 Badge)
    // ============================================================
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('notification:view')")
    public ResponseEntity<ApiResponseDto<NotificationCountDto>> getUnreadCount() {
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (currentUserId == null || currentUserId <= 0L) {
            return ResponseEntity.status(401)
                    .body(ApiResponseDto.error(401, "請先登入"));
        }
        long count = notificationService.getUnreadCount(currentUserId);
        return ResponseEntity.ok(ApiResponseDto.ok(new NotificationCountDto(count)));
    }

    // ============================================================
    // ✅ 標記已讀
    // ============================================================
    @PatchMapping("/{userNotificationId}/read")
    @PreAuthorize("hasAuthority('notification:view')")
    public ResponseEntity<ApiResponseDto<Void>> markAsRead(@PathVariable Long userNotificationId) {
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (currentUserId == null || currentUserId <= 0L) {
            return ResponseEntity.status(401)
                    .body(ApiResponseDto.error(401, "請先登入"));
        }
        notificationService.markAsRead(currentUserId, userNotificationId);
        return ResponseEntity.ok(ApiResponseDto.ok(null));
    }
}