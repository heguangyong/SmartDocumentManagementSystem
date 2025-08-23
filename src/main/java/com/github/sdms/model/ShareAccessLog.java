package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "share_access_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;         // 明文 token（可选，仅开发或调试使用）

    private String tokenHash;     // SHA-256 摘要或加密后的摘要

    private Long fileId;

    private String fileName;

    private String accessIp;      // 明文 IP（可选）
    private String accessIpEnc;   // KMS 加密后的 IP

    private String userAgent;     // 明文 UA（可选）
    private String userAgentEnc;  // KMS 加密后的 UA

    @Temporal(TemporalType.TIMESTAMP)
    private Date accessTime = new Date();

    private String libraryCode;

    @Column(name = "owner_id")
    private Long ownerId; // 用户表 user.id

    private String actionType; // preview, download, list

    private String signature; // 签名结果

}
