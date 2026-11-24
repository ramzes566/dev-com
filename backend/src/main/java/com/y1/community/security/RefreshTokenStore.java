package com.y1.community.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenStore {

    private static final String PREFIX = "auth:refresh:";

    private final StringRedisTemplate redis;

    public RefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void save(String jti, Long userId, Duration ttl) {
        redis.opsForValue().set(PREFIX + jti, String.valueOf(userId), ttl);
    }

    public boolean consume(String jti) {
        return Boolean.TRUE.equals(redis.delete(PREFIX + jti));
    }

    public void revoke(String jti) {
        redis.delete(PREFIX + jti);
    }
}
