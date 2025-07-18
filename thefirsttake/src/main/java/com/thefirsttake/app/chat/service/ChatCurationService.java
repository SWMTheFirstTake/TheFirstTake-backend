package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ChatCurationService {
    private final RestTemplate restTemplate;

    @Value("${llm.server.host}")
    private String llmServerHost;

    @Value("${llm.server.port}")
    private String llmServerPort;

    // 새로운 전문가 체인 API 호출 메서드
    public List<Map<String, Object>> getExpertChainResult(String userInput, Long roomId, String promptValue) {
        try {
            // 요청 데이터 구성 (FastAPI의 ExpertChainRequest 형태)
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("user_input", userInput);
            requestMap.put("room_id", roomId);

            // 전문가 순서 설정 (기본값 사용 또는 커스텀)
            List<String> expertSequence = Arrays.asList(
                    "style_analyst",
                    "color_expert",
                    "trend_expert",
                    "fitting_coordinator"
            );
            requestMap.put("expert_sequence", expertSequence);

            // 사용자 프로필 정보가 있다면 추가
            Map<String, Object> userProfile = new HashMap<>();
            // userProfile.put("age", "20대");
            // userProfile.put("gender", "남성");
            requestMap.put("user_profile", userProfile);

            // 컨텍스트 정보
            Map<String, Object> contextInfo = new HashMap<>();
            contextInfo.put("previous_conversation", promptValue);
            requestMap.put("context_info", contextInfo);

            // FastAPI 전문가 체인 API 호출
//            String fastApiUrl = "http://" + llmServerHost + ":" + llmServerPort + "/api/expert/chain";
            String fastApiUrl = "http://" + "3.35.85.182" + ":" + "6020" + "/api/expert/chain";
            ResponseEntity<ApiResponse> response = restTemplate.postForEntity(fastApiUrl, requestMap, ApiResponse.class);

            ApiResponse body = response.getBody();
            if (body == null || body.getData() == null) {
                throw new RuntimeException("FastAPI 서버로부터 유효한 응답을 받지 못했습니다.");
            }

            // 응답 데이터 파싱
            Map<String, Object> responseData = (Map<String, Object>) body.getData();
            List<Map<String, Object>> expertAnalyses = (List<Map<String, Object>>) responseData.get("expert_analyses");

            if (expertAnalyses == null || expertAnalyses.isEmpty()) {
                throw new RuntimeException("전문가 분석 결과가 비어있습니다.");
            }

            return expertAnalyses;

        } catch (Exception e) {
            System.err.println("전문가 체인 API 호출 실패: " + e.getMessage());
            throw new RuntimeException("전문가 체인 분석 실패", e);
        }
    }
    // 프롬프트 추가 문구를 Map으로 관리
    private static final Map<Integer, String> CURATION_PROMPT_ADDITIONS;

    static {
        Map<Integer, String> tempMap = new HashMap<>();
        tempMap.put(1, "라는 사용자의 질문에 대해 웬만하면 포멀한 스타일로 스타일 한 개만 추천해줘");
        tempMap.put(2, "라는 사용자의 질문에 대해 웬만하면 캐쥬얼한 스타일로 스타일 한 개만 추천해줘");
        tempMap.put(3, "라는 사용자의 질문에 대해 웬만하면 시티보이 스타일로 스타일 한 개만 추천해줘");
        CURATION_PROMPT_ADDITIONS = Collections.unmodifiableMap(tempMap); // 불변 Map으로 생성
    }

    @Async // 이 메서드를 비동기로 실행
    public CompletableFuture<String> getResult(String promptKey, String promptValue, int curationNumber) {
        String promptAddition = CURATION_PROMPT_ADDITIONS.getOrDefault(curationNumber, "");
        promptValue += promptAddition;

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("prompt", promptValue);

        String fastApiUrl = "http://" + llmServerHost + ":" + llmServerPort + "/api/ask";
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(fastApiUrl, requestMap, ApiResponse.class);

        ApiResponse body = response.getBody();
        if (body == null || body.getData() == null) {
            // 로깅 추가 (예: log.error("FastAPI 서버로부터 유효한 응답을 받지 못했습니다. 응답 본문: {}", response.getBody());)
            return CompletableFuture.completedFuture("FastAPI 서버로부터 유효한 응답을 받지 못했습니다.");
        }
        return CompletableFuture.completedFuture(body.getData().toString());
    }
}
