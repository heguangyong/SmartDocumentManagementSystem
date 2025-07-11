package com.github.sdms.dto;

import com.github.sdms.util.MapUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户请求对象，用于处理认证、上传等场景的统一参数封装
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UUserReq {

    // 用户身份信息
    private String uid;
    private String code;

    // 上传相关
    private Long fileSize;
    private String fileName;
    private String uploadId;
    private String dbfilename;

    // 排序/过滤控制
    private String order;
    private String asc;

    // 显示信息
    private String token;
    private String name;
    private String username;

    /**
     * 将非空字段填充为 Map（用于签名/验证等场景）
     */
    public Map<String, Object> getMap() {
        Map<String, Object> map = new HashMap<>();
        MapUtils.setIfPresent(map, "uid", uid);
        MapUtils.setIfPresent(map, "code", code);
        MapUtils.setIfPresent(map, "fileSize", fileSize);
        MapUtils.setIfPresent(map, "fileName", fileName);
        MapUtils.setIfPresent(map, "uploadId", uploadId);
        MapUtils.setIfPresent(map, "dbfilename", dbfilename);

        MapUtils.setIfPresent(map, "token", token);
        MapUtils.setIfPresent(map, "asc", asc);
        MapUtils.setIfPresent(map, "name", name);
        MapUtils.setIfPresent(map, "username", username);
        MapUtils.setIfPresent(map, "order", order);
        return map;
    }
}
