package com.github.sdms.dto;

import lombok.Data;

@Data
public class LibrarySiteDTO {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String type;
    private Boolean status;
}
