package com.github.sdms.dto;

import lombok.Data;

@Data
public class UserFilePageRequest {
    private int page = 0;        // 页码，从0开始
    private int size = 10;       // 每页数量
    private String name;         // 文件或目录名模糊搜索
    private String type;         // 文件类型过滤
    private Long folderId;       // 所属目录ID
    private String libraryCode;  // 馆代码
    private String keyword;  // 支持文件名/目录名的模糊搜索

}
