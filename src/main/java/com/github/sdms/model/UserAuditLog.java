package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "user_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long  userId;         // uid
    private String username;
    private String libraryCode;
    private String ip;
    private String userAgent;

    private String actionType;     // LOGIN_SUCCESS, LOGIN_FAIL, PASSWORD_CHANGE, etc.
    @Column(columnDefinition = "TEXT")
    private String actionDetail;   // 可存储 JSON

    private Date createdTime;

    @Column(length = 2048)
    private String signature; // 国密签名数据
}
