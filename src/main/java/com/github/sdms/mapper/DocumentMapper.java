package com.github.sdms.mapper;

import com.github.sdms.dto.CreateDTO;
import com.github.sdms.dto.DocumentDTO;
import com.github.sdms.dto.DocumentVersionDTO;
import com.github.sdms.model.Document;
import com.github.sdms.model.DocumentVersion;
import com.github.sdms.model.AppUser;

import java.time.LocalDateTime;

public class DocumentMapper {

    public static DocumentDTO toDTO(Document document){
        return DocumentDTO.builder()
                .id(document.getId())
                .name(document.getName())
                .path(document.getPath())
                .uploadTime(document.getUploadTime())
                .userId(document.getUser() != null ? document.getUser().getId() : null)
                .userName(document.getUser() != null ? document.getUser().getUsername() : null )
                .build();
    }

    public static Document toEntity(CreateDTO dto, AppUser user ){
        return Document.builder()
                .name(dto.getName())
                .path(dto.getPath())
                .uploadTime(LocalDateTime.now())
                .user(user)
                .build();
    }

    public static DocumentVersionDTO toVersionDTO(DocumentVersion version) {
        return DocumentVersionDTO.builder()
                .id(version.getId())
                .fileName(version.getFileName())
                .notes(version.getNotes())
                .versionNumber(version.getVersionNumber())
                .uploadTime(version.getUploadTime())
                .build();
    }
}
