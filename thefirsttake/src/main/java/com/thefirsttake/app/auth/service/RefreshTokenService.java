package com.thefirsttake.app.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final StringRedisTemplate stringRedisTemplate;

    private String buildKey(String userId) {
        // Key format: {user_id}:refresh_token (per requested convention)
        return userId + ":refresh_token";
    }

    public void saveRefreshToken(String userId, String refreshToken, Duration ttl) {
        String key = buildKey(userId);
        stringRedisTemplate.opsForValue().set(key, refreshToken, ttl);
    }

    public String getRefreshToken(String userId) {
        String key = buildKey(userId);
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void deleteRefreshToken(String userId) {
        String key = buildKey(userId);
        stringRedisTemplate.delete(key);
    }
}
