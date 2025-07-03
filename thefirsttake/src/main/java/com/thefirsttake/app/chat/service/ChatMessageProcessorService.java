package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class ChatMessageProcessorService {
    private final PromptValueService promptValueService;
    private final CurationResultService curationResultService;
    private final PromptCacheService promptCacheService;
    public List<String> generateCurationResponse(String userInput, Long roomId) {
        String promptKey=roomId+":prompt";
        // 1. redis에 promptKey를 key로 프롬프트를 가져오거나 or 새로운 프롬프트 생성
        String currentPromptValue=promptValueService.getPrompt(promptKey,userInput);

        // 2. 프롬프트를 바탕으로 큐레이션 결과를 가져옴
        // 첫 번째 AI 포멀한 옷 추천
        String formalCurationResult=curationResultService.getResult(promptKey,currentPromptValue,1);
        formalCurationResult+="1번째 AI";

        // 두 번째 AI 캐쥬얼한 옷 추천
//        currentPromptValue=promptValueService.getPrompt(promptKey,userInput);
        String casualCurationResult=curationResultService.getResult(promptKey,currentPromptValue,2);
        casualCurationResult+="2번째 AI";

        // 세 번째 AI 시티보이 옷 추천
//        currentPromptValue=promptValueService.getPrompt(promptKey,userInput);
        String cityBoyCurationResult=curationResultService.getResult(promptKey,currentPromptValue,3);
        cityBoyCurationResult+="3번째 AI";
        // 3. 프롬프트밸류+현재 큐레이션 결과를 저장
        promptCacheService.savePrompt(promptKey,currentPromptValue+formalCurationResult+casualCurationResult+cityBoyCurationResult);

        // 4. 큐레이션 결과를 사용자에게 보여주기 위해 보여줌
        List<String> curationResults=new ArrayList<>();
        curationResults.add(formalCurationResult);
        curationResults.add(casualCurationResult);
        curationResults.add(cityBoyCurationResult);
        return curationResults;
    }
}
