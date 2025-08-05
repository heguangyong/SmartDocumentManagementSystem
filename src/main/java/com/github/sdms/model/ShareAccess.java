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

    private String token;         // 明文 token，仅用于传输
    private String tokenHash;     // 哈希 token，用于校验

    private String targetType;    // "file" 或 "folder"
    private Long targetId;        // 对应 fileId 或 folderId
    private String targetName;    // 可选：用于前端显示

    private Long ownerId;     // 创建者 对应 user表id
    private String libraryCode;   // 所属库

    private Date expireAt;
    private Date createdAt;

    private Boolean enabled = true;
}
