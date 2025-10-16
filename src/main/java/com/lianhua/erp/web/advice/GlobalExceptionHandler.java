package com.lianhua.erp.web.advice;

import com.lianhua.erp.dto.apiResponse.ApiResponseDto;
import com.lianhua.erp.dto.error.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================================
    // 400：參數或驗證錯誤
    // ============================================================
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<BadRequestResponse> handleBadRequest(Exception ex) {
        String msg = (ex instanceof MethodArgumentNotValidException e)
                ? e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .collect(Collectors.joining(", "))
                : ex.getMessage();
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
    // 409：資料衝突（名稱重複 / 外鍵約束 / 唯一鍵違反）
    // ============================================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ConflictResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "資料重複或違反唯一約束條件";

        String exceptionMsg = ex.getMessage() != null ? ex.getMessage() : "";
        String rootMsg = (ex.getRootCause() != null && ex.getRootCause().getMessage() != null)
                ? ex.getRootCause().getMessage()
                : "";

        // 1️⃣ 自定義唯一鍵：商品名稱
        if (exceptionMsg.contains("uq_product_name")) {
            message = "商品名稱重複，請重新輸入不同名稱。";
        }

        // 2️⃣ 自定義唯一鍵：供應商名稱
        else if (exceptionMsg.contains("uk_supplier_name")) {
            message = "供應商名稱重複，請重新輸入不同名稱。";
        }

        // 3️⃣ 其他 Duplicate entry 一般情況（MySQL 原生訊息）
        else if (rootMsg.contains("Duplicate entry")) {
            message = extractDuplicateMessage(rootMsg);
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConflictResponse(message));
    }

    /**
     * 🔧 解析 MySQL Duplicate entry 錯誤訊息
     * 例如：
     * Duplicate entry 'jacob' for key 'users.username'
     */
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
    // 500：伺服器內部錯誤（未捕捉）
    // ============================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<InternalServerErrorResponse> handleServerError(Exception ex) {
        ex.printStackTrace(); // ✅ 開發階段方便除錯
        String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new InternalServerErrorResponse(msg));
    }
}
