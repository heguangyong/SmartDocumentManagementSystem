package com.github.sdms.controller;


import com.github.sdms.dto.DocumentDTO;
import com.github.sdms.dto.DocumentVersionDTO;
import com.github.sdms.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,@RequestParam("userId") Long userId) {
        try {
            documentService.uploadDocument(file, userId);
            return ResponseEntity.ok("File uploaded successfully ! ");
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> listDouments(){
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        try {
            byte[] data = documentService.downloadDocument(id);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=document_" + id)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable Long id) throws IOException {
        documentService.deleteDocument(id);
        return ResponseEntity.ok("Deleted successfully");
    }

    @PostMapping("/{id}/versions")
    public ResponseEntity<DocumentVersionDTO> uploadNewVersion(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "notes", required = false) String versionNotes) {
        try {
            return ResponseEntity.ok(documentService.uploadNewVersion(file, id, versionNotes));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<DocumentVersionDTO>> getVersions(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentVersions(id));
    }

    @GetMapping("/{id}/versions/{versionId}")
    public ResponseEntity<byte[]> downloadVersion(@PathVariable Long id, @PathVariable Long versionId) {
        try {
            byte[] data = documentService.downloadDocumentVersion(versionId);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=version_" + versionId)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentDTO>> searchDocuments(@RequestParam String query) {
        return ResponseEntity.ok(documentService.searchDocuments(query));
    }
}
