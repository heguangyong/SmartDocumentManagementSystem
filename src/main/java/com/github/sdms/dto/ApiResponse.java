package com.github.sdms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用API响应封装类
 * @param <T> 返回数据类型
 */
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

    // 静态快捷方法

    /**
     * 返回成功且带数据
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data);
    }

    /**
     * 返回成功带自定义消息和数据
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * ✅ 返回成功（无数据，默认消息）
     */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, "操作成功", null);
    }

    /**
     * 返回失败带自定义消息
     */
    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, message, null);
    }

    /**
     * 返回失败带消息和数据（少用）
     */
    public static <T> ApiResponse<T> failure(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
}
