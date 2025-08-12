package com.github.sdms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// 创建存储桶请求类
public class CreateBucketRequest {
    @Schema(description = "桶所有者ID", example = "1001")
    @NotNull(message = "所有者ID不能为空")
    private Long ownerId;

    @Schema(description = "桶名称（管理员可指定）", example = "archive-2023")
    @Pattern(regexp = "^[a-z0-9-]{3,64}$", message = "桶名需为3-64位小写字母、数字或横线")
    private String name;

    @Schema(description = "最大容量（单位：GB）", example = "10")
    @Min(value = 1, message = "容量至少1GB")
    @Max(value = 1024, message = "单桶最大1024GB")
    private Integer maxCapacityGB; // 改为Integer类型

    @Schema(description = "描述信息", example = "年度归档存储")
    @Size(max = 255, message = "描述最长255字符")
    private String description;
}

