package com.lianhua.erp.web.advice;

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

/**
 * 全域例外處理器，統一封裝所有錯誤。
 * 對應各種 HTTP 狀態碼：400, 403, 404, 409, 500。
 */
@Hidden // 避免 Swagger 掃描 /v3/api-docs 時報錯
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===============================
    // 🔹 400：請求參數或驗證錯誤
    // ===============================
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

    // ===============================
    // 🔹 403：禁止存取（權限不足）
    // ===============================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ForbiddenResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ForbiddenResponse("無權限存取此資源"));
    }

    // ===============================
    // 🔹 404：找不到資源
    // ===============================
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<NotFoundResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new NotFoundResponse(ex.getMessage()));
    }

    // ===============================
    // 🔹 409：資料衝突
    // ===============================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BadRequestResponse> handleDataConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new BadRequestResponse("資料違反約束：" + ex.getMostSpecificCause().getMessage()));
    }

    // ===============================
    // 🔹 401：登入認證錯誤
    // ===============================
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ForbiddenResponse> handleAuthError(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ForbiddenResponse("認證失敗：" + ex.getMessage()));
    }

    // ===============================
    // 🔹 500：伺服器內部錯誤（兜底）
    // ===============================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<InternalServerErrorResponse> handleServerError(Exception ex) {
        String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new InternalServerErrorResponse(msg));
    }
}
