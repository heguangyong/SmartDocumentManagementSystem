package com.github.sdms.exception;

import lombok.Getter;

/**
 * 业务异常：用于统一处理前端友好的错误提示
 */
@Getter
public class ApiException extends RuntimeException {
    /** 业务错误码，对应 ApiResponse.code */
    private final int code;

    public ApiException(String message) {
        this(400, message);
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }
}

