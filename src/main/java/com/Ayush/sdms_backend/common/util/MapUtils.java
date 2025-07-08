package com.Ayush.sdms_backend.common.util;

import cn.hutool.core.util.ObjectUtil;

import java.util.Map;

public class MapUtils {

    /**
     * 向 Map 中添加非 null 值，空字符串也添加
     */
    public static void setIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * 向 Map 中添加非 null 且非空字符串的值
     */
    public static void setIfNotBlank(Map<String, Object> map, String key, Object value) {
        if (value instanceof String) {
            if (value != null && !((String) value).trim().isEmpty()) {
                map.put(key, value);
            }
        } else if (ObjectUtil.isNotEmpty(value)) {
            map.put(key, value);
        }
    }
}
