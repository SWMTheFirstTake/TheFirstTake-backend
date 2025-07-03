package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurationResultService {
    private final RestTemplate restTemplate;

    @Value("${llm.server.host}")
    private String llmServerHost;

    @Value("${llm.server.port}")
    private String llmServerPort;

    public String getResult(String promptKey, String promptValue, int curationNumber){
        if(curationNumber==1){
            promptValue+="라는 사용자의 질문에 대해 웬만하면 포멀한 스타일로 스타일 한 개만 추천해줘";
        }else if(curationNumber==2){
            promptValue+="라는 사용자의 질문에 대해 웬만하면 캐쥬얼한 스타일로 스타일 한 개만 추천해줘";
        }else if(curationNumber==3){
            promptValue+="라는 사용자의 질문에 대해 웬만하면 시티보이 스타일로 스타일 한 개만 추천해줘";
        }
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("prompt", promptValue);

        String fastApiUrl = "http://"+llmServerHost+":"+llmServerPort+"/api/ask";
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(fastApiUrl, requestMap, ApiResponse.class);

        ApiResponse body = response.getBody();
        if (body == null || body.getData() == null) {
            return "FastAPI 서버로부터 유효한 응답을 받지 못했습니다.";
        }
        return body.getData().toString();
    }
}
