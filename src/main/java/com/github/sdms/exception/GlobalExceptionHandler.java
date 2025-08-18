package com.github.sdms.exception;

import com.github.sdms.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常（自定义异常）
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .contentType(MediaType.APPLICATION_JSON) // 👈 强制返回 JSON
                .body(ApiResponse.failure(ex.getMessage()));
    }

    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        String message = ex.getMessage();
        // 这里根据实际情况定制消息或统一提示
        if (message != null && message.contains("ADMIN")) {
            message = "权限不足，只有管理员可以执行此操作";
        } else {
            message = "权限不足，您没有执行该操作的权限";
        }
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON) // 👈 强制返回 JSON
                .body(ApiResponse.failure(message));
    }


    /**
     * 处理所有未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        ex.printStackTrace(); // 日志记录建议保留
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON) // 👈 强制返回 JSON
                .body(ApiResponse.failure("服务器内部错误，请稍后再试"));
    }
}
