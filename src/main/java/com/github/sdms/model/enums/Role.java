package com.github.sdms.model.enums;

public enum Role {
    READER, //普通用户（READER）：只能访问自己 UID 所属桶（用户级别）
    LIBRARIAN, //管理员（LIBRARIAN）：可以访问本馆（libraryCode）下所有用户桶
    ADMIN //超级管理员（ADMIN）：可以访问任意馆下的桶
}
