package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageProcessorService {
    private final PromptValueService promptValueService;
    private final CurationResultService curationResultService;
    private final PromptCacheService promptCacheService;
    public List<String> generateCurationResponse(String userInput, Long roomId) {
        String promptKey=roomId+":prompt";
        // 1. redis에 promptKey를 key로 프롬프트를 가져오거나 or 새로운 프롬프트 생성
        String currentPromptValue=promptValueService.getPrompt(promptKey,userInput,1);

        // 2. 프롬프트를 바탕으로 큐레이션 결과를 가져옴
        // 첫 번째 AI
        String curationResult=curationResultService.getResult(promptKey,currentPromptValue);
        curationResult+="1번째 AI";
        // 두 번째 AI
        currentPromptValue=promptValueService.getPrompt(promptKey,userInput,2);
        String tmpCurationResult=curationResultService.getResult(promptKey,currentPromptValue);
        tmpCurationResult+="2번째 AI";

        // 세 번째 AI
        currentPromptValue=promptValueService.getPrompt(promptKey,userInput,3);
        String cityBoyCurationResult=curationResultService.getResult(promptKey,currentPromptValue);
        cityBoyCurationResult+="3번째 AI"
        // 3. 프롬프트밸류+현재 큐레이션 결과를 저장
        promptCacheService.savePrompt(promptKey,currentPromptValue+curationResult+tmpCurationResult+currentPromptValue);

        // 4. 큐레이션 결과를 사용자에게 보여주기 위해 보여줌
        List<String> curationResults=new ArrayList<>();
        curationResults.add(curationResult);
        curationResults.add(tmpCurationResult);
        return curationResults;
    }
}
