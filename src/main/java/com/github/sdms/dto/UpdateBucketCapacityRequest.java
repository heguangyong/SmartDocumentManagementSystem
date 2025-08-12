package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
// 更新容量请求类
public class UpdateBucketCapacityRequest {
    @Schema(description = "最大容量（单位：GB）", example = "20")
    @Min(value = 1)
    @Max(value = 1024)
    private Integer maxCapacityGB;
}
