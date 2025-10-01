package com.thefirsttake.app.chat.service;

import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 채팅 프롬프트 관리 전담 서비스
 * - 프롬프트 저장/조회
 * - 프롬프트 초기화
 * - Redis 기반 프롬프트 캐싱
 */
@Service
@Slf4j
public class ChatPromptService {
    private final StringRedisTemplate redisTemplate;
    
    public ChatPromptService(@Qualifier("stringRedisTemplate") StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 프롬프트 키 존재 여부 확인
     */
    public boolean hasPromptKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 프롬프트 저장
     */
    public void savePrompt(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
        log.debug("프롬프트 저장 완료: key={}", key);
    }

    /**
     * 프롬프트 조회
     */
    public String getPrompt(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 프롬프트가 없으면 생성하고, 있으면 기존 프롬프트와 사용자 입력을 결합
     */
    public String getOrCreatePrompt(String promptKey, String userInput) {
        if (hasPromptKey(promptKey)) {
            // 기존 프롬프트와 사용자 입력 결합
            String savedPrompt = getPrompt(promptKey);
            return savedPrompt + userInput;
        } else {
            // 새 프롬프트 생성
            String newPrompt = initializeBasePrompt() + userInput;
            savePrompt(promptKey, newPrompt);
            return newPrompt;
        }
    }

    /**
     * 기본 프롬프트 초기화
     */
    private String initializeBasePrompt() {
        return "시나리오: 당신은 패션 큐레이터입니다. 사용자가 옷을 잘 모르기 때문에, 옷을 고를 때 최소한의 선택만 하도록 돕는 것이 목적입니다. 그래서 사용자가 원하는 취향을 유도하기 위한 답을 주어야 합니다. 다음은 사용자의 질문입니다.";
    }

    /**
     * 프롬프트 삭제
     */
    public void deletePrompt(String key) {
        redisTemplate.delete(key);
        log.debug("프롬프트 삭제 완료: key={}", key);
    }
} 