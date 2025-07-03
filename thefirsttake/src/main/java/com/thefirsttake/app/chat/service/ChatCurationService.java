package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatCurationService {
    private final RestTemplate restTemplate;

    @Value("${llm.server.host}")
    private String llmServerHost;

    @Value("${llm.server.port}")
    private String llmServerPort;

    // 프롬프트 추가 문구를 Map으로 관리
    private static final Map<Integer, String> CURATION_PROMPT_ADDITIONS;

    static {
        Map<Integer, String> tempMap = new HashMap<>();
        tempMap.put(1, "라는 사용자의 질문에 대해 웬만하면 포멀한 스타일로 스타일 한 개만 추천해줘");
        tempMap.put(2, "라는 사용자의 질문에 대해 웬만하면 캐쥬얼한 스타일로 스타일 한 개만 추천해줘");
        tempMap.put(3, "라는 사용자의 질문에 대해 웬만하면 시티보이 스타일로 스타일 한 개만 추천해줘");
        CURATION_PROMPT_ADDITIONS = Collections.unmodifiableMap(tempMap); // 불변 Map으로 생성
    }

    public String getResult(String promptKey, String promptValue, int curationNumber){
        // curationNumber에 해당하는 프롬프트 추가 문구 가져오기
        String promptAddition = CURATION_PROMPT_ADDITIONS.getOrDefault(curationNumber, ""); // 기본값은 빈 문자열
        promptValue += promptAddition; // 프롬프트에 추가

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("prompt", promptValue);

        String fastApiUrl = "http://"+llmServerHost+":"+llmServerPort+"/api/ask";
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(fastApiUrl, requestMap, ApiResponse.class);

        ApiResponse body = response.getBody();
        if (body == null || body.getData() == null) {
            // 로깅을 추가하여 어떤 경우에 유효하지 않은 응답을 받았는지 추적하는 것이 좋습니다.
            // log.error("FastAPI 서버로부터 유효한 응답을 받지 못했습니다. 응답 본문: {}", response.getBody());
            return "FastAPI 서버로부터 유효한 응답을 받지 못했습니다.";
        }
        return body.getData().toString();
    }
}
