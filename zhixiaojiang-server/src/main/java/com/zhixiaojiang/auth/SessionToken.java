package com.zhixiaojiang.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 会话令牌（HS256 紧凑序列化格式）的签发与校验。
 *
 * <p>令牌以 HttpOnly Cookie 传递；有效期内每次登录都带唯一 {@code jti}，
 * 保证「退出后立刻重新登录」不会生成与已撤销令牌相同的字符串。
 */
public final class SessionToken {
    /** 令牌有效期：8 小时，与登录 Cookie 的 Max-Age 保持一致。 */
    public static final long TTL_SECONDS = 28800;

    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private SessionToken() {
    }

    /** 签发令牌：{@code header.payload.signature}。 */
    public static String issue(long userId, String username, String role, String secret) {
        String header = encode(HEADER);
        String payload = encode("{\"jti\":\"" + UUID.randomUUID()
                + "\",\"sub\":\"" + userId
                + "\",\"username\":\"" + username
                + "\",\"role\":\"" + role
                + "\",\"exp\":" + (Instant.now().getEpochSecond() + TTL_SECONDS) + "}");
        return header + "." + payload + "." + sign(header + "." + payload, secret);
    }

    /** 使用 HMAC-SHA256 计算前两段的签名。 */
    public static String sign(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return encodeBytes(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("会话令牌签名失败", e);
        }
    }

    private static String encode(String value) {
        return encodeBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String encodeBytes(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
