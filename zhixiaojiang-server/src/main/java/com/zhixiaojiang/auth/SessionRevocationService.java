package com.zhixiaojiang.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话撤销标记：退出登录后，被撤销的令牌必须立即失效。
 *
 * <p>实现分两层，任何时候都不再静默失败：
 * <ol>
 *   <li>本实例内存表是权威来源。即使 Redis 不可用，当前进程也会在令牌剩余有效期内拒绝该会话，
 *       因此「退出登录」在本机永远生效。</li>
 *   <li>Redis 只是跨实例共享记录。写入或读取失败会记录 WARN，并说明影响范围。</li>
 * </ol>
 *
 * <p>已知边界：单实例部署下上述两层等价。若将来横向扩容到多个实例，Redis 读取失败时其他实例
 * 可能仍接受已被撤销的会话；那时必须把 Redis 变为强依赖（读取失败直接拒绝请求），而不是接受会话。
 * 当前比赛版为单实例部署，因此选择「本实例内存优先 + 明确告警」。
 *
 * <p>只保存令牌的 SHA-256 指纹，从不持久化原始会话令牌。
 */
@Service
public class SessionRevocationService {
  private static final Logger log = LoggerFactory.getLogger(SessionRevocationService.class);
  /** 内存表容量上限；超过后按到期时间淘汰最早的标记。 */
  private static final int LOCAL_LIMIT = 20000;

  private final StringRedisTemplate redis;
  private final boolean enabled;
  private final String prefix;
  private final Map<String, Long> localRevoked = new ConcurrentHashMap<>();
  /** Redis 故障告警节流，避免每个请求都刷日志。 */
  private final java.util.concurrent.atomic.AtomicLong lastWarningAt = new java.util.concurrent.atomic.AtomicLong();

  public SessionRevocationService(StringRedisTemplate redis, org.springframework.core.env.Environment env) {
    this.redis = redis;
    this.enabled = env.getProperty("app.redis-enabled", Boolean.class, true);
    this.prefix = env.getProperty("app.redis-prefix", "zhixiaojiang:");
  }

  /** 记录撤销标记：先写本实例内存，再尽力同步到 Redis。 */
  public void revoke(String token, Duration ttl) {
    if (token == null || token.isBlank() || ttl == null || ttl.isZero() || ttl.isNegative()) return;
    String fingerprint = fingerprint(token);
    localRevoked.put(fingerprint, System.currentTimeMillis() + ttl.toMillis());
    evictIfNeeded();
    if (!enabled) {
      log.warn("Redis 未启用，会话撤销标记仅保存在本实例内存中：{}", shortFingerprint(fingerprint));
      return;
    }
    try {
      redis.opsForValue().set(key(fingerprint), "1", ttl);
    } catch (RuntimeException e) {
      warnThrottled("Redis 写入会话撤销标记失败，该令牌仍会在本实例失效，但多实例部署下其他实例可能继续接受它：{}", e.toString());
    }
  }

  /** 判断令牌是否已被撤销：本实例内存优先，其次查询 Redis。 */
  public boolean isRevoked(String token) {
    if (token == null || token.isBlank()) return false;
    String fingerprint = fingerprint(token);
    Long expiresAt = localRevoked.get(fingerprint);
    if (expiresAt != null) {
      if (expiresAt > System.currentTimeMillis()) return true;
      localRevoked.remove(fingerprint, expiresAt);
    }
    if (!enabled) return false;
    try {
      return Boolean.TRUE.equals(redis.hasKey(key(fingerprint)));
    } catch (RuntimeException e) {
      warnThrottled("Redis 读取会话撤销标记失败，本次只依据本实例内存判断；多实例部署下已被其他实例撤销的会话可能被接受：{}", e.toString());
      return false;
    }
  }

  /** 同一故障在 60 秒内只记录一条 WARN，但仍保留可见性。 */
  private void warnThrottled(String message, Object detail) {
    long now = System.currentTimeMillis();
    long previous = lastWarningAt.get();
    if (now - previous < 60_000) return;
    if (lastWarningAt.compareAndSet(previous, now)) log.warn(message, detail);
  }

  /** 仅供测试与运维观察当前内存标记数量。 */
  public int localMarkerCount() {
    return localRevoked.size();
  }

  private void evictIfNeeded() {
    long now = System.currentTimeMillis();
    localRevoked.entrySet().removeIf(entry -> entry.getValue() <= now);
    if (localRevoked.size() <= LOCAL_LIMIT) return;
    log.warn("本实例撤销标记已达上限 {}，按到期时间淘汰最早的标记", LOCAL_LIMIT);
    localRevoked.entrySet().stream()
        .sorted(Comparator.comparingLong(Map.Entry::getValue))
        .limit(localRevoked.size() - LOCAL_LIMIT)
        .map(Map.Entry::getKey)
        .toList()
        .forEach(localRevoked::remove);
  }

  private String key(String fingerprint) {
    return prefix + "session:revoked:" + fingerprint;
  }

  private String shortFingerprint(String fingerprint) {
    return fingerprint.substring(0, 12) + "…";
  }

  private String fingerprint(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte b : digest) hex.append(String.format("%02x", b));
      return hex.toString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
