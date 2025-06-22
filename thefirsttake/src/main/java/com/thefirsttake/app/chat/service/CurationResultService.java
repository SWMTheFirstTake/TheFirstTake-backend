package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurationResultService {
    private final RestTemplate restTemplate;
    public String getResult(String promptKey, String promptValue){
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("prompt", promptValue);

        String fastApiUrl = "http://localhost:6020/api/ask";
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(fastApiUrl, requestMap, ApiResponse.class);

        ApiResponse body = response.getBody();
        if (body == null || body.getData() == null) {
            return "FastAPI 서버로부터 유효한 응답을 받지 못했습니다.";
        }
        return body.getData().toString();
    }
}
