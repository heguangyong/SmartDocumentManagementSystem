package com.github.sdms.dto;

import lombok.Data;

@Data
public class FolderPageRequest {
    private Integer page = 0;
    private Integer size = 10;
    private String keyword;
}
