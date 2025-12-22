package com.lianhua.erp.web.advice;

import com.lianhua.erp.dto.error.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================================
    // 400：參數或格式錯誤（含日期/型別錯誤）
    // ============================================================
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            DateTimeParseException.class
    })
    public ResponseEntity<BadRequestResponse> handleBadRequest(Exception ex) {
        String msg;

        if (ex instanceof MethodArgumentNotValidException e) {
            msg = e.getBindingResult().getFieldErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)   // ⭐ 修正，避免出現 name prefix
                    .collect(Collectors.joining(", "));
        } else if (ex instanceof MethodArgumentTypeMismatchException e) {
            msg = String.format("參數 %s 格式錯誤，期望型別為 %s",
                    e.getName(),
                    e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        } else if (ex instanceof DateTimeParseException) {
            msg = "日期格式錯誤，請使用 yyyy-MM-dd 或 yyyy-MM 格式";
        } else {
            msg = ex.getMessage();
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BadRequestResponse(msg));
    }

    // ============================================================
    // 401：認證錯誤
    // ============================================================
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<UnauthorizedResponse> handleAuthError(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new UnauthorizedResponse("認證失敗：" + ex.getMessage()));
    }

    // ============================================================
    // 403：權限不足
    // ============================================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ForbiddenResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ForbiddenResponse("無權限存取此資源"));
    }

    // ============================================================
    // 404：找不到資源
    // ============================================================
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<NotFoundResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new NotFoundResponse(ex.getMessage()));
    }

    // ============================================================
    // 409：資料衝突（唯一鍵 / 外鍵）
    // ============================================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ConflictResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "資料重複或違反唯一約束條件";

        String exceptionMsg = ex.getMessage() != null ? ex.getMessage() : "";
        String rootMsg = (ex.getRootCause() != null && ex.getRootCause().getMessage() != null)
                ? ex.getRootCause().getMessage()
                : "";

        if (exceptionMsg.contains("uq_product_name")) {
            message = "商品名稱重複，請重新輸入不同名稱。";
        } else if (exceptionMsg.contains("uk_supplier_name")) {
            message = "供應商名稱重複，請重新輸入不同名稱。";
        } else if (rootMsg.contains("Duplicate entry")) {
            message = extractDuplicateMessage(rootMsg);
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConflictResponse(message));
    }

    private String extractDuplicateMessage(String dbMessage) {
        try {
            int entryStart = dbMessage.indexOf("Duplicate entry '") + 17;
            int entryEnd = dbMessage.indexOf("'", entryStart);
            String duplicateValue = dbMessage.substring(entryStart, entryEnd);

            int keyStart = dbMessage.indexOf("for key '") + 9;
            int keyEnd = dbMessage.indexOf("'", keyStart);
            String keyName = dbMessage.substring(keyStart, keyEnd);

            return "欄位 " + keyName + " 的值已存在：" + duplicateValue;
        } catch (Exception e) {
            return "資料重複或違反唯一約束條件";
        }
    }

    // ============================================================
    // 409：一般業務邏輯衝突（IllegalArgument）
    // ============================================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ConflictResponse> handleConflict(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConflictResponse(ex.getMessage()));
    }

    // ============================================================
    // 400：業務狀態錯誤（IllegalState）
    // ============================================================
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<BadRequestResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BadRequestResponse(ex.getMessage()));
    }

    // ============================================================
    // 500：伺服器內部錯誤
    // ============================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<InternalServerErrorResponse> handleServerError(Exception ex) {
        ex.printStackTrace(); // ✅ 開發階段方便除錯
        String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new InternalServerErrorResponse(msg));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<BaseErrorResponse> handleResponseStatusException(ResponseStatusException ex) {

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : "請求錯誤";

        return ResponseEntity
                .status(status)
                .body(new BadRequestResponse(message));
    }
}
