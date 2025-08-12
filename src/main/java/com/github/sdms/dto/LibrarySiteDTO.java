package com.github.sdms.dto;

import lombok.Data;

/**
 * 馆点数据传输对象（DTO）
 * <p>
 * 用于前后端交互时传递馆点信息，
 * 通常用于馆点的查询结果、创建和更新请求。
 *
 * 与 {@link com.github.sdms.model.LibrarySite} 实体对应，
 * 但不包含数据库自动维护的字段（如 createTime、updateTime）。
 */
@Data
public class LibrarySiteDTO {

    /** 馆点ID（主键） */
    private Long id;

    /** 馆点编码（唯一），例如：MAIN_LIB_001 */
    private String code;

    /** 馆点名称，例如：主馆、分馆一 */
    private String name;

    /** 馆点地址 */
    private String address;

    /** 馆点类型（可选，如 MAIN 主馆 / BRANCH 分馆） */
    private String type;

    /** 启用状态（true=启用，false=停用） */
    private Boolean status;
}
