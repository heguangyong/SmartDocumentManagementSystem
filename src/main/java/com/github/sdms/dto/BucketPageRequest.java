package com.github.sdms.dto;

import lombok.Data;

@Data
public class BucketPageRequest {
    private int page = 1;
    private int size = 10;
    private String keyword; // 支持桶名或UID模糊搜索
}
