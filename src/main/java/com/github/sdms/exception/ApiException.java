package com.github.sdms.exception;

import lombok.Getter;

/**
 * 业务异常：用于统一处理前端友好的错误提示
 */
@Getter
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(String message) {
        this(400, message);
    }

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }
}
