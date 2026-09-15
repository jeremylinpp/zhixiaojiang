package com.zhixiaojiang.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 统一响应结构：{@code {code, message, data, requestId}}。
 *
 * <p>所有接口的成功与失败响应都经由此类构造，禁止在控制器里各自拼装响应 Map。
 */
public final class ApiResult {
    public static final String SUCCESS_CODE = "0";
    public static final String SUCCESS_MESSAGE = "success";

    private ApiResult() {
    }

    /** 成功响应；data 必须是已构造好的对象（Map/List/record 均可）。 */
    public static Map<String, Object> ok(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", SUCCESS_CODE);
        body.put("message", SUCCESS_MESSAGE);
        body.put("data", data);
        body.put("requestId", UUID.randomUUID().toString());
        return body;
    }

    /** 失败响应；code 使用 HTTP 状态码字符串，data 固定为空对象。 */
    public static Map<String, Object> fail(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", Map.of());
        body.put("requestId", UUID.randomUUID().toString());
        return body;
    }
}
