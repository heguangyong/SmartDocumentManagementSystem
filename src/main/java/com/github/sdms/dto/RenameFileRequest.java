package com.github.sdms.dto;

import com.drew.lang.annotations.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RenameFileRequest {
    @NotNull
    private Long fileId;
    @NotBlank
    private String newName;
    // getter/setter
}
