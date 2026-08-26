package com.bikeshop.common.security;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Revogação de token (logout) via blacklist no Redis, chaveada pelo claim {@code jti}. O sistema é
 * stateless por padrão (JWT auto-contido) — sem isso, um token vazado continuaria válido até
 * expirar mesmo após logout. A entrada expira sozinha (TTL = tempo restante até a expiração do
 * token), então a blacklist nunca cresce além do necessário.
 */
@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "revoked-jwt:";

    private final RedisTemplate<String, Object> redisTemplate;

    public TokenBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void revoke(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, Boolean.TRUE, ttl);
    }

    public boolean isRevoked(String jti) {
        return jti != null && Boolean.TRUE.equals(redisTemplate.opsForValue().get(KEY_PREFIX + jti));
    }
}
