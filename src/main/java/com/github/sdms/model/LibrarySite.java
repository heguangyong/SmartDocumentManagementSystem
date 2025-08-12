package com.github.sdms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 馆点实体类
 * <p>
 * 对应表：library_site
 * 用于存储图书馆的基本信息，如编码、名称、地址、类型等。
 * 后台管理系统可对该表进行增删改查，用于用户、文件等业务模块的关联。
 *
 * @author
 */
@Entity
@Table(name = "library_site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySite {
    /** 主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 馆点编码（唯一），例如：MAIN_LIB_001 */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** 馆点名称，例如：主馆、分馆一 */
    @Column(nullable = false)
    private String name;

    /** 馆点地址，支持详细地址描述 */
    private String address;

    /** 馆点类型（可选，如：MAIN 主馆 / BRANCH 分馆） */
    private String type;

    /** 启用状态（true=启用，false=停用），默认启用 */
    private Boolean status = true;

    /** 排序值（越小越靠前），默认 0 */
    private Integer sortOrder = 0;

    /** 创建时间（由 Hibernate 自动填充） */
    @CreationTimestamp
    private LocalDateTime createTime;

    /** 最后更新时间（由 Hibernate 自动更新） */
    @UpdateTimestamp
    private LocalDateTime updateTime;
}
