package com.thefirsttake.app.flow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptCacheService {
    private final StringRedisTemplate redisTemplate;

    public boolean hasPromptKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void savePrompt(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String getPrompt(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
