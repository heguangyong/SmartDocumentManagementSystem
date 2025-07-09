package com.github.sdms.dto;

import lombok.Data;

@Data
public class CreateDTO {
    private String name;
    private String path;
    private Long userId;
}
