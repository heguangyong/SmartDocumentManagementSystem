package com.github.sdms.dto;

import lombok.Data;

import java.util.Date;

@Data
public class FolderSummaryDTO {
    private Long id;
    private String name;
    private Long parentId;
    private Boolean isPublic;
    private Boolean shared;
    private Date createdDate; // DTO中仍用 createdDate
}



