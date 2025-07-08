package com.Ayush.sdms_backend.mapper;

import com.Ayush.sdms_backend.dto.CreateDTO;
import com.Ayush.sdms_backend.dto.DocumentDTO;
import com.Ayush.sdms_backend.dto.DocumentVersionDTO;
import com.Ayush.sdms_backend.model.Document;
import com.Ayush.sdms_backend.model.DocumentVersion;
import com.Ayush.sdms_backend.model.AppUser;

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
