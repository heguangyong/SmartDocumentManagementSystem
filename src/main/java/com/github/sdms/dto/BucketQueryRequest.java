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
public class BucketQueryRequest {

    @Schema(description = "桶名关键字（模糊搜索）")
    private String nameKeyword;

    @Schema(description = "馆代码")
    private String libraryCode;

    @Schema(description = "用户ID（ownerId）")
    private Long ownerId;

    @Schema(description = "页码，从 1 开始")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
