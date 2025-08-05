package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateBucketCapacityRequest {

    @Schema(description = "最大容量，单位：字节（建议前端显示 MB）")
    private Long maxCapacity;
}
