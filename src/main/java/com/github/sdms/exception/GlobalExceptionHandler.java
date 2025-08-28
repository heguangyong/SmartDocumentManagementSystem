package com.github.sdms.exception;

import com.github.sdms.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        // 直接返回 HTTP 200，但 JSON 里有真实业务 code
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApiResponse<>(false, ex.getMessage(), null, ex.getCode()));
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        String message = ex.getMessage();
        int code = 403;
        if (message != null && message.contains("ADMIN")) {
            message = "权限不足，只有管理员可以执行此操作";
        } else {
            message = "权限不足，您没有执行该操作的权限";
        }
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.failure(message, code));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        ex.printStackTrace();
        int code = 500;
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.failure("服务器内部错误，请稍后再试", code));
    }

}
