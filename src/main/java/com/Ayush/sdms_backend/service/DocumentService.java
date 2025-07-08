package com.Ayush.sdms_backend.service;

import com.Ayush.sdms_backend.dto.DocumentDTO;
import com.Ayush.sdms_backend.dto.DocumentVersionDTO;
import com.Ayush.sdms_backend.exception.UserNotFoundException;
import com.Ayush.sdms_backend.mapper.DocumentMapper;
import com.Ayush.sdms_backend.model.Document;
import com.Ayush.sdms_backend.model.DocumentVersion;
import com.Ayush.sdms_backend.model.AppUser;
import com.Ayush.sdms_backend.repository.DocumentRepository;
import com.Ayush.sdms_backend.repository.DocumentVersionRepository;
import com.Ayush.sdms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final DocumentVersionRepository documentVersionRepository;
    private final Tika tika = new Tika();
    // private final ElasticsearchOperations elasticsearchOperations; // Uncomment and configure if ElasticSearch is ready

    public DocumentDTO uploadDocument(MultipartFile file, Long userId) throws IOException {
        // File handling
        String originalFilename = file.getOriginalFilename();
        String s3Key = s3Service.uploadFile(file);

        // Document creation
        AppUser user = userRepository.findById(userId)
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

    // retrieving and listing all meta data
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

    // give metadata for the document with specific id
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

    // Versioning logic
    public DocumentVersionDTO uploadNewVersion(MultipartFile file, Long documentId, String notes) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));
        int nextVersion = document.getVersions() == null ? 1 : document.getVersions().size() + 1;
        String fileName = file.getOriginalFilename();
        String s3Key = s3Service.uploadFile(file);
        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .fileName(fileName)
                .s3Key(s3Key)
                .uploadTime(LocalDateTime.now())
                .notes(notes)
                .versionNumber(nextVersion)
                .build();
        documentVersionRepository.save(version);
        // Tika extract (for future ElasticSearch indexing)
        String text = "";
        try {
            text = tika.parseToString(file.getInputStream());
        } catch (Exception e) {
            // log or handle extraction error
        }
        // TODO: Index text in ElasticSearch
        return DocumentMapper.toVersionDTO(version);
    }

    public List<DocumentVersionDTO> getDocumentVersions(Long documentId) {
        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream()
                .map(DocumentMapper::toVersionDTO)
                .collect(Collectors.toList());
    }

    public byte[] downloadDocumentVersion(Long versionId) throws IOException {
        DocumentVersion version = documentVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found with id: " + versionId));
        return s3Service.downloadFile(version.getS3Key());
    }

    public List<DocumentDTO> searchDocuments(String query) {
        // TODO: Implement ElasticSearch search logic
        return List.of(); // Placeholder
    }
}