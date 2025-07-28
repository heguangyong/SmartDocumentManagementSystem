package com.github.sdms.model.enums;

import com.github.sdms.exception.ApiException;

public enum RoleType {
    READER, //普通用户（READER）：只能访问自己 UID 所属桶（用户级别）
    LIBRARIAN, //管理员（LIBRARIAN）：可以访问本馆（libraryCode）下所有用户桶
    ADMIN; //超级管理员（ADMIN）：可以访问任意馆下的桶

    public static RoleType fromString(String value) {
        try {
            return RoleType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new ApiException("角色名无效：" + value);
        }
    }
}
