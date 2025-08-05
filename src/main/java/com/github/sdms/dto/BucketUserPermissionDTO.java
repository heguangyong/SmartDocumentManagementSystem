package com.github.sdms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BucketUserPermissionDTO {
    private Long userId;         // user.id
    private String username;     // user.username
    private String roleType;     // user.roleType
    private String permission;   // VIEW / EDIT
}
