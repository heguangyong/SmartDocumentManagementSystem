package com.github.sdms.dto;

import com.drew.lang.annotations.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RenameFolderRequest {
    @NotNull
    private Long folderId;

    @NotBlank
    private String newName;

    // getter & setter
}