package com.github.sdms.controller;

import com.github.sdms.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    @Operation(summary = "用户上传文件（自动绑定当前用户）")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("uid") String uidFromClient) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName(); // JWT中的sub字段（uid）
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

        boolean isAdmin = authorities.stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUsername.equals(uidFromClient)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure("You are not allowed to upload files for other users"));
        }

        // TODO: 将文件存储逻辑集成 MinioClientService.upload(file, uid)
        log.info("[UPLOAD] user={} uploading file: {}", currentUsername, file.getOriginalFilename());

        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully (mocked)."));
    }
}
