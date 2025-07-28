package com.github.sdms.model.enums;

import com.github.sdms.exception.ApiException;

public enum PermissionType {
    READ,
    WRITE,
    DELETE; // 可根据需要添加

    public static PermissionType fromString(String value) {
        try {
            return PermissionType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new ApiException("权限名无效：" + value);
        }
    }
}
