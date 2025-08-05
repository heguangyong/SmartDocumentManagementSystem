package com.github.sdms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BucketSummaryDTO {
    private Long id;
    private String name;
    private Long ownerId;
    private Date createTime;
    private Integer accessUserCount;
    private Long usedCapacity; // 单位：Byte
    private Long maxCapacity;  // 单位：Byte
}


