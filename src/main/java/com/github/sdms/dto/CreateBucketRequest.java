package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateBucketRequest {

    @Schema(description = "桶所有者ID，user.id")
    private Long ownerId;

    @Schema(description = "最大容量（单位：字节）")
    private Long maxCapacity;

    @Schema(description = "备注或描述")
    private String description;
}