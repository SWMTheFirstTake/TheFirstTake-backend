package com.thefirsttake.app.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptValueService {
    private final PromptCacheService promptCacheService;
    public String getPrompt(String promptKey, String userInput){
        String promptValue="";
        if(promptCacheService.hasPromptKey(promptKey)){
            // 존재함
            String savedPrompt = promptCacheService.getPrompt(promptKey);
            promptValue=savedPrompt+userInput;
        }else{
            promptValue = "시나리오: 당신은 패션 큐레이터입니다. 사용자가 옷을 잘 모르기 때문에, 옷을 고를 때 최소한의 선택만 하도록 돕는 것이 목적입니다. 그래서 사용자가 원하는 취향을 유도하기 위한 답을 주어야 합니다. 다음은 질문에 대한 답을 해주세요";
            promptValue=promptValue+userInput;
        }
        return promptValue;
    }
}
