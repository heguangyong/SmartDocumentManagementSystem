package com.Ayush.sdms_backend.controller;


//import com.Ayush.sdms_backend.components.JwtUtil;
import com.Ayush.sdms_backend.dto.DocumentDTO;
import com.Ayush.sdms_backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
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

    @Autowired
    private DocumentService documentService;

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
}
