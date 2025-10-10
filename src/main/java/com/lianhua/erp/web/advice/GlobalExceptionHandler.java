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

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 400：參數或驗證錯誤
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
    
    // 401：認證失敗
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<UnauthorizedResponse> handleAuthError(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new UnauthorizedResponse("認證失敗：" + ex.getMessage()));
    }
    
    // 403：權限不足
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ForbiddenResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ForbiddenResponse("無權限存取此資源"));
    }
    
    // 404：找不到資源
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<NotFoundResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new NotFoundResponse(ex.getMessage()));
    }
    
    // 409：資料衝突或違反約束
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ConflictResponse> handleConflict(DataIntegrityViolationException ex) {
        String rootCauseMsg = ex.getMostSpecificCause().getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConflictResponse("⚠️ 資料衝突：" + rootCauseMsg));
    }
    
    // 500：伺服器內部錯誤
    @ExceptionHandler(Exception.class)
    public ResponseEntity<InternalServerErrorResponse> handleServerError(Exception ex) {
        String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new InternalServerErrorResponse(msg));
    }
}
