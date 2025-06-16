package com.Ayush.sdms_backend.mapper;

import com.Ayush.sdms_backend.dto.CreateDTO;
import com.Ayush.sdms_backend.dto.DocumentDTO;
import com.Ayush.sdms_backend.model.Document;
import com.Ayush.sdms_backend.model.User;

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

    public static Document toEntity(CreateDTO dto, User user ){
        return Document.builder()
                .name(dto.getName())
                .path(dto.getPath())
                .uploadTime(LocalDateTime.now())
                .user(user)
                .build();
    }
}
