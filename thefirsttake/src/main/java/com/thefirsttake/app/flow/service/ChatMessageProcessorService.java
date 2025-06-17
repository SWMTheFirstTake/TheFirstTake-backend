package com.thefirsttake.app.flow.service;

import com.thefirsttake.app.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatMessageProcessorService {
    private final PromptValueService promptValueService;
    private final CurationResultService curationResultService;
    private final PromptCacheService promptCacheService;
    public String generateCurationResponse(String userInput, String currentSession) {
        String promptKey=currentSession+":prompt";
        // 1. redis에 promptKey를 key로 프롬프트를 가져오거나 or 새로운 프롬프트 생성
        String currentPromptValue=promptValueService.getPrompt(promptKey,userInput);

        // 2. 프롬프트를 바탕으로 큐레이션 결과를 가져옴
        String curationResult=curationResultService.getResult(promptKey,currentPromptValue);

        // 3. 프롬프트밸류+현재 큐레이션 결과를 저장
        promptCacheService.savePrompt(promptKey,currentPromptValue+curationResult);
        // 4. 큐레이션 결과를 사용자에게 보여주기 위해 보여줌
        return curationResult;
    }
}
