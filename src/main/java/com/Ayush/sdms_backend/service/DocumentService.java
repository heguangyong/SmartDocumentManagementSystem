package com.Ayush.sdms_backend.service;

import com.Ayush.sdms_backend.dto.DocumentDTO;
import com.Ayush.sdms_backend.exception.UserNotFoundException;
import com.Ayush.sdms_backend.mapper.DocumentMapper;
import com.Ayush.sdms_backend.model.Document;
import com.Ayush.sdms_backend.model.User;
import com.Ayush.sdms_backend.repository.DocumentRepository;
import com.Ayush.sdms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public DocumentDTO uploadDocument(MultipartFile file, Long userId) throws IOException {

        // File handling
        String originalFilename = file.getOriginalFilename();
        String s3Key = s3Service.uploadFile(file);

        // Document creation
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Document document = Document.builder()
                .name(originalFilename)
                .path(s3Key)
                .uploadTime(LocalDateTime.now())
                .user(user)
                .build();

        Document savedDocument = documentRepository.save(document);
        return DocumentMapper.toDTO(savedDocument);
    }

//    retriving and listing all meta data
    public List<DocumentDTO> getAllDocuments() {
        return documentRepository.findAll()
                .stream()
                .map(DocumentMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteDocument(Long id) throws IOException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        // Delete file from s3
        s3Service.deleteFile(document.getPath());

        // Delete from database
        documentRepository.delete(document);
    }

//    give metadata for the document with specific id
    public DocumentDTO getDocumentById(Long id) {
        return documentRepository.findById(id)
                .map(DocumentMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
    }

    public byte[] downloadDocument(Long id) throws IOException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        return s3Service.downloadFile(document.getPath());
    }
}