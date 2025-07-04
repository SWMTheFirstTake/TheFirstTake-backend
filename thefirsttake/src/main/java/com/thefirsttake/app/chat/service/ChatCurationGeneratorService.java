package com.thefirsttake.app.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


@Service
@RequiredArgsConstructor
public class ChatCurationGeneratorService {
    private final ChatCurationService chatCurationService;
    private final ChatPromptManageService chatPromptManageService;
    public List<String> generateCurationResponse(String userInput, Long roomId) {
//        String promptKey=roomId+":prompt";
//        // 1. redis에 promptKey를 key로 프롬프트를 가져오거나 or 새로운 프롬프트 생성
//        String currentPromptValue= chatPromptManageService.getOrCreatePrompt(promptKey,userInput);
//
//        // 2. 프롬프트를 바탕으로 큐레이션 결과를 가져옴
//        // 첫 번째 AI 포멀한 옷 추천
//        String formalCurationResult= chatCurationService.getResult(promptKey,currentPromptValue,1);
//        formalCurationResult+="1번째 AI";
//
//         // 두 번째 AI 캐쥬얼한 옷 추천
//        String casualCurationResult= chatCurationService.getResult(promptKey,currentPromptValue,2);
//        casualCurationResult+="2번째 AI";
//
//        // 세 번째 AI 시티보이 옷 추천
//        String cityBoyCurationResult= chatCurationService.getResult(promptKey,currentPromptValue,3);
//        cityBoyCurationResult+="3번째 AI";
//
//        // 3. 프롬프트밸류+현재 큐레이션 결과를 저장
//        chatPromptManageService.savePrompt(promptKey,currentPromptValue+formalCurationResult+casualCurationResult+cityBoyCurationResult);
//
//        // 4. 큐레이션 결과를 사용자에게 보여주기 위해 보여줌
//        List<String> curationResults=new ArrayList<>();
//        curationResults.add(formalCurationResult);
//        curationResults.add(casualCurationResult);
//        curationResults.add(cityBoyCurationResult);
//        return curationResults;
        String promptKey = roomId + ":prompt";
        String currentPromptValue = chatPromptManageService.getOrCreatePrompt(promptKey, userInput);

        // 2. 프롬프트를 바탕으로 큐레이션 결과를 비동기로 가져옴
        CompletableFuture<String> formalCurationFuture = chatCurationService.getResult(promptKey, currentPromptValue, 1);
        CompletableFuture<String> casualCurationFuture = chatCurationService.getResult(promptKey, currentPromptValue, 2);
        CompletableFuture<String> cityBoyCurationFuture = chatCurationService.getResult(promptKey, currentPromptValue, 3);

        List<String> curationResults = new ArrayList<>();
        StringBuilder promptUpdateBuilder = new StringBuilder(currentPromptValue); // 프롬프트 업데이트를 위한 StringBuilder

        try {
            // 모든 비동기 작업이 완료될 때까지 대기하고 결과를 가져옴
            String formalCurationResult = formalCurationFuture.get();
            formalCurationResult += "1번째 AI";
            curationResults.add(formalCurationResult);
            promptUpdateBuilder.append(formalCurationResult);

            String casualCurationResult = casualCurationFuture.get();
            casualCurationResult += "2번째 AI";
            curationResults.add(casualCurationResult);
            promptUpdateBuilder.append(casualCurationResult);

            String cityBoyCurationResult = cityBoyCurationFuture.get();
            cityBoyCurationResult += "3번째 AI";
            curationResults.add(cityBoyCurationResult);
            promptUpdateBuilder.append(cityBoyCurationResult);

        } catch (InterruptedException | ExecutionException e) {
            // 예외 처리 (로깅 등을 추가하여 어떤 예외가 발생했는지 기록)
            System.err.println("비동기 큐레이션 결과 처리 중 오류 발생: " + e.getMessage());
            // 필요한 경우 사용자에게 보여줄 에러 메시지를 추가하거나 기본값 처리
            curationResults.add("큐레이션 결과를 가져오는 중 오류가 발생했습니다.");
            curationResults.add("큐레이션 결과를 가져오는 중 오류가 발생했습니다.");
            curationResults.add("큐레이션 결과를 가져오는 중 오류가 발생했습니다.");
        }

        // 3. 프롬프트밸류+현재 큐레이션 결과를 저장
        chatPromptManageService.savePrompt(promptKey, promptUpdateBuilder.toString());

        // 4. 큐레이션 결과를 사용자에게 보여주기 위해 보여줌
        return curationResults;
    }

}
