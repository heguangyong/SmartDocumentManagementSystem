package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBucketRequest {
    @Schema(description = "桶ID", example = "b15")
    private Long id;
    @Schema(description = "桶名称（管理员可指定）", example = "archive-2023")
    @Pattern(regexp = "^[a-z0-9-]{3,64}$", message = "桶名需为3-64位小写字母、数字或横线")
    private String name;
    @Schema(description = "描述信息", example = "年度归档存储")
    @Size(max = 255, message = "描述最长255字符")
    private String description;
    @Schema(description = "最大容量（单位：GB）", example = "20")
    @Min(value = 1)
    @Max(value = 1024)
    private Integer maxCapacityGB;
}
