package com.thefirsttake.app.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


@Service
@RequiredArgsConstructor
public class ChatCurationGeneratorService {
    private final ChatCurationService chatCurationService;
    private final ChatPromptManageService chatPromptManageService;
    public List<String> generateCurationResponse(String userInput, Long roomId) {
        String promptKey = roomId + ":prompt";
        String currentPromptValue = chatPromptManageService.getOrCreatePrompt(promptKey, userInput);
        try {
            // ⭐ 새로운 전문가 체인 API 1번 호출로 대체
            List<Map<String, Object>> expertAnalyses = chatCurationService.getExpertChainResult(
                    userInput, roomId, currentPromptValue
            );

            List<String> curationResults = new ArrayList<>();
            StringBuilder promptUpdateBuilder = new StringBuilder(currentPromptValue);
            List<String> curationTypes=new ArrayList<>();
            curationTypes.add("저는 체형분석과 핏감을 중심으로 추천을 해드려요!");
            curationTypes.add("저는 최신트렌드, 인플루언서의 스타일을 중심으로 추천을 해드려요!");
            curationTypes.add("저는 피부톤에 어울리는 색상 조합을 바탕으로 추천을 해드려요!");
            curationTypes.add("저는 종합적으로 딱 하나의 추천을 해드려요!");
            // 전문가별 결과 처리 (기존 try-catch 블록의 내용을 대체)
            for (int i = 0; i < expertAnalyses.size(); i++) {
                Map<String, Object> expertResult = expertAnalyses.get(i);
                String expertType = (String) expertResult.get("expert_type");
                String analysis = (String) expertResult.get("analysis");
                String expertRole = (String) expertResult.get("expert_role");

                // 기존 형태와 유사하게 포맷팅
//                String formattedResult = analysis + " (" + (i + 1) + "번째 AI)";
                String formattedResult = curationTypes.get(i)+analysis;

                curationResults.add(formattedResult);
                promptUpdateBuilder.append(formattedResult);
            }

            // 기존과 동일: 프롬프트 업데이트
            chatPromptManageService.savePrompt(promptKey, promptUpdateBuilder.toString());

            return curationResults;

        } catch (Exception e) {
            // 기존과 동일한 예외 처리
            System.err.println("전문가 체인 분석 중 오류 발생: " + e.getMessage());

            List<String> errorResults = new ArrayList<>();
            errorResults.add("큐레이션 결과를 가져오는 중 오류가 발생했습니다.");
            errorResults.add("큐레이션 결과를 가져오는 중 오류가 발생했습니다.");
            errorResults.add("큐레이션 결과를 가져오는 중 오류가 발생했습니다.");
            return errorResults;
        }
//        // 2. 프롬프트를 바탕으로 큐레이션 결과를 비동기로 가져옴
//        CompletableFuture<String> formalCurationFuture = chatCurationService.getResult(promptKey, currentPromptValue, 1);
//        CompletableFuture<String> casualCurationFuture = chatCurationService.getResult(promptKey, currentPromptValue, 2);
//        CompletableFuture<String> cityBoyCurationFuture = chatCurationService.getResult(promptKey, currentPromptValue, 3);
//
//        List<String> curationResults = new ArrayList<>();
//        StringBuilder promptUpdateBuilder = new StringBuilder(currentPromptValue); // 프롬프트 업데이트를 위한 StringBuilder
//
//        try {
//            // 모든 비동기 작업이 완료될 때까지 대기하고 결과를 가져옴
//            String formalCurationResult = formalCurationFuture.get();
//            formalCurationResult += "1번째 AI";
//            curationResults.add(formalCurationResult);
//            promptUpdateBuilder.append(formalCurationResult);
//
//            String casualCurationResult = casualCurationFuture.get();
//            casualCurationResult += "2번째 AI";
//            curationResults.add(casualCurationResult);
//            promptUpdateBuilder.append(casualCurationResult);
//
//            String cityBoyCurationResult = cityBoyCurationFuture.get();
//            cityBoyCurationResult += "3번째 AI";
//            curationResults.add(cityBoyCurationResult);
//            promptUpdateBuilder.append(cityBoyCurationResult);
//
//        } catch (InterruptedException | ExecutionException e) {
//            // 예외 처리 (로깅 등을 추가하여 어떤 예외가 발생했는지 기록)
//            System.err.println("비동기 큐레이션 결과 처리 중 오류 발생: " + e.getMessage());
//            // 필요한 경우 사용자에게 보여줄 에러 메시지를 추가하거나 기본값 처리
//            curationResults.add("큐레이션 결과를 가져오는 중 오류가 발생했습니다.");
//            curationResults.add("큐레이션 결과를 가져오는 중 오류가 발생했습니다.");
//            curationResults.add("큐레이션 결과를 가져오는 중 오류가 발생했습니다.");
//        }
//
//        // 3. 프롬프트밸류+현재 큐레이션 결과를 저장
//        chatPromptManageService.savePrompt(promptKey, promptUpdateBuilder.toString());

        // 4. 큐레이션 결과를 사용자에게 보여주기 위해 보여줌
//        return curationResults;
    }

}
