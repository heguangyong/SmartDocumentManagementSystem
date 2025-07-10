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

    private String token;

    private Long fileId;

    private String fileName;

    private String accessIp;

    private String userAgent;

    @Temporal(TemporalType.TIMESTAMP)
    private Date accessTime = new Date();
}
