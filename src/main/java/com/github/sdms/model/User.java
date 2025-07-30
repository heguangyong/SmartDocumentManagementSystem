package com.github.sdms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.sdms.model.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user")
@Schema(description = "统一用户实体类（OAuth + 本地）")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "主键ID，自增长")
    private Long id;

    @Column(unique = true)
    @Schema(description = "上图统一用户UID，OAuth唯一标识")
    private String uid;

    @Column(nullable = false)
    @Schema(description = "用户名，唯一")
    private String username;

    // 新增馆代码字段
    @Column(nullable = false)
    @Schema(description = "馆代码")
    private String libraryCode;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "密码（已加密存储）")
    private String password;

    @Schema(description = "密码盐")
    private String salt;

    @Column(unique = true)
    @Schema(description = "电子邮箱，唯一")
    private String email;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "用户等级")
    private Byte level;

    @Schema(description = "性别，0未知，1男，2女")
    private Byte gender;

    @Schema(description = "生日，日期格式")
    private Date birthday;

    @Schema(description = "个性签名/格言")
    private String bio;

    @Schema(description = "积分")
    private Integer score;

    @Schema(description = "连续登录天数")
    private Integer successions;

    @Schema(description = "最大连续登录天数")
    private Integer maxsuccessions;

    @Schema(description = "上次登录时间，时间戳")
    private Long prevtime;

    @Schema(description = "当前登录时间，时间戳")
    private Long logintime;

    @Schema(description = "登录IP地址")
    private String loginip;

    @Schema(description = "登录失败次数")
    private Byte loginfailure;

    @Schema(description = "注册IP地址")
    private String joinip;

    @Schema(description = "注册时间，时间戳")
    private Long jointime;

    @Schema(description = "创建时间，时间戳")
    private Long createtime;

    @Schema(description = "更新时间，时间戳")
    private Long updatetime;

    @Schema(description = "用户Token")
    private String token;

    @Schema(description = "用户状态，例：active/inactive/locked")
    private String status;

    @Schema(description = "验证状态，例：已验证、未验证")
    private String verification;

    @Schema(description = "openid（第三方授权用）")
    private String openid;

    @Schema(description = "unionid（第三方授权用）")
    private String unionid;

    @Schema(description = "真实姓名")
    private String realname;

    @Schema(description = "身份证号码")
    private String idcard;

    @Schema(description = "创建事件，日期格式")
    private Date createdAt;

    @Schema(description = "是否关注公众号，0未关注，1关注")
    private Integer subscribe;

    @Lob
    @Column(columnDefinition = "TEXT")
    @Schema(description = "用户详细信息JSON字符串")
    private String userinfo;

    @Schema(description = "是否同意隐私协议，0不同意，1同意")
    private Integer privacycheck;

    @Schema(description = "来源IP")
    private String ip;

    @Enumerated(EnumType.STRING)
    @Column(name = "role",nullable = false)
    @Builder.Default // 默认构造时自动使用 READER
    private RoleType roleType = RoleType.READER;// READER, LIBRARIAN, ADMIN


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    @Schema(description = "关联的文档列表")
    private List<Document> documents;
}
