package com.thefirsttake.app.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatPromptManageService {
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

    public String getOrCreatePrompt(String promptKey, String userInput){
        String promptValue;
        if(hasPromptKey(promptKey)){
            // 존재하면 캐시된 프롬프트와 사용자 입력을 결합
            String savedPrompt = getPrompt(promptKey);
            promptValue = savedPrompt + userInput;
        } else {
            // 존재하지 않으면 기본 프롬프트 생성 후 사용자 입력을 결합
            promptValue = initializeBasePrompt() + userInput; // 초기 프롬프트 생성 로직 분리
            // 여기서는 getOrCreatePrompt가 생성과 동시에 저장까지 하지는 않습니다.
            // 필요하다면 savePrompt(promptKey, promptValue)를 추가할 수 있습니다.
        }
        return promptValue;
    }
    /**
     * 기본 프롬프트 시나리오를 초기화하는 메서드 (새로 생성될 때 사용)
     */
    private String initializeBasePrompt() {
        return "시나리오: 당신은 패션 큐레이터입니다. 사용자가 옷을 잘 모르기 때문에, 옷을 고를 때 최소한의 선택만 하도록 돕는 것이 목적입니다. 그래서 사용자가 원하는 취향을 유도하기 위한 답을 주어야 합니다. 다음은 사용자의 질문입니다.";
    }
}
