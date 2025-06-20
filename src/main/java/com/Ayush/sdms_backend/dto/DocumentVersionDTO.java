package com.Ayush.sdms_backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersionDTO {
    private Long id;
    private String fileName;
    private String notes;
    private Integer versionNumber;
    private LocalDateTime uploadTime;
} 