package com.zhixiaojiang.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/** JSON 列的读写：只处理本系统使用的数组结构，异常时返回空集合而非中断业务。 */
public final class JsonValues {
    private static final ObjectMapper JSON = new ObjectMapper();

    private JsonValues() {
    }

    /** 序列化为 JSON 文本；失败时返回空数组，避免写入非法列值。 */
    public static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** 读取 JSON 数组；为空或解析失败时返回空列表。 */
    public static List<Object> toList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return JSON.readValue(raw, new TypeReference<List<Object>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
