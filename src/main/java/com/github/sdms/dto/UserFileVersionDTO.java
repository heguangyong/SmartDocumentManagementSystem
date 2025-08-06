package com.github.sdms.dto;

import lombok.Data;

import java.util.Date;

@Data
public class UserFileVersionDTO {
    private Long id;
    private Integer versionNumber;
    private String versionKey;
    private String originFilename;
    private Long size;
    private Date createdDate;
    private Boolean isLatest;
}

