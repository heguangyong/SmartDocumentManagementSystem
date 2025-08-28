package com.github.sdms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    /** 是否成功 */
    private boolean success;

    /** 提示信息 */
    private String message;

    /** 具体数据 */
    private T data;

    /** 业务状态码 */
    private int code;

    // 成功返回（默认 code = 0）
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data, 200);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, 200);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, "操作成功", null, 200);
    }

    // 失败返回，带自定义 code
    public static <T> ApiResponse<T> failure(String message, int code) {
        return new ApiResponse<>(false, message, null, code);
    }

    public static <T> ApiResponse<T> failure(String message, T data, int code) {
        return new ApiResponse<>(false, message, data, code);
    }
}
