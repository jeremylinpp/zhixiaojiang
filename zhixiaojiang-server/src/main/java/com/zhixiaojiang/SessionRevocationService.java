package com.zhixiaojiang;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

/** Stores revoked JWT fingerprints without ever persisting the raw session token. */
@Service
public class SessionRevocationService {
  private final StringRedisTemplate redis;
  private final boolean enabled;
  private final String prefix;

  public SessionRevocationService(StringRedisTemplate redis, org.springframework.core.env.Environment env) {
    this.redis = redis;
    this.enabled = env.getProperty("app.redis-enabled", Boolean.class, true);
    this.prefix = env.getProperty("app.redis-prefix", "zhixiaojiang:");
  }

  public void revoke(String token, Duration ttl) {
    if (!enabled || token == null || token.isBlank()) return;
    try { redis.opsForValue().set(key(token), "1", ttl); } catch (RuntimeException ignored) { /* Redis outage must not break logout. */ }
  }

  public boolean isRevoked(String token) {
    if (!enabled || token == null || token.isBlank()) return false;
    try { return Boolean.TRUE.equals(redis.hasKey(key(token))); } catch (RuntimeException ignored) { return false; }
  }

  private String key(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte b : digest) hex.append(String.format("%02x", b));
      return prefix + "session:revoked:" + hex;
    } catch (Exception e) { throw new IllegalStateException(e); }
  }
}
