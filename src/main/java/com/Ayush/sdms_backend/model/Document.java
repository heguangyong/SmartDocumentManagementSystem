package com.Ayush.sdms_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "document")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "file_path", nullable = false, length = 255)
    private String path;

    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"documents"})
    private AppUser user;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentVersion> versions;

}
