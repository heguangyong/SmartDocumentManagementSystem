package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "share_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;         // 明文 token（可选，建议前端只接收一次性返回）
    private String tokenHash;     // 哈希化存储 token，校验用

    private String type;          // "file" 或 "folder"

    private Long targetId;        // 指向 fileId 或 folderId
    private String targetName;    // 文件或目录名

    private String ownerUid;
    private String libraryCode;

    private Date expireAt;
    private Date createdAt = new Date();

    private Boolean active = true;
}

