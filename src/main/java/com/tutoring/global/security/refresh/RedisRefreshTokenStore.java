package com.tutoring.global.security.refresh;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redis;

    @Override
    public void save(Long userId, String token, Duration ttl) {
        redis.opsForValue().set(key(userId), token, ttl);
    }

    @Override
    public Optional<String> find(Long userId) {
        return Optional.ofNullable(redis.opsForValue().get(key(userId)));
    }

    @Override
    public void delete(Long userId) {
        redis.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
