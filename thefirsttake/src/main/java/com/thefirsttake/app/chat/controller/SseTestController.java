package com.thefirsttake.app.chat.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/sse-test")
@CrossOrigin(origins = "*")
public class SseTestController {

    private final RestTemplate restTemplate;
    private final ExecutorService executorService;
    
    @Value("${llm.server.expert-stream-url}")
    private String llmExpertStreamUrl;

    public SseTestController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.executorService = Executors.newCachedThreadPool();
    }

    @GetMapping("/expert-stream")
    public SseEmitter testExpertStream(
        @RequestParam("user_input") String userInput,
        @RequestParam("expert_type") String expertType,
        @RequestParam("room_id") int roomId,
        @RequestParam(value = "user_profile", required = false) String userProfile
    ) {
        SseEmitter emitter = new SseEmitter(300000L); // 5분 타임아웃
        
        // SSE 헤더 설정
        try {
            emitter.send(SseEmitter.event()
                .name("connect")
                .data("SSE 연결 성공"));
        } catch (IOException e) {
            System.out.println("⚠️ 초기 SSE 메시지 전송 실패: " + e.getMessage());
        }
        
        executorService.execute(() -> {
            try {
                System.out.println("🚀 SSE 테스트 시작");
                System.out.println("📤 요청 데이터: user_input=" + userInput + ", expert_type=" + expertType + ", room_id=" + roomId + ", user_profile=" + (userProfile == null ? "(없음)" : userProfile));
                
                // 1단계: 매칭 시작 알림
                sendSseMessage(emitter, "status", "착장 매칭 시작...", 1);
                
                // 2단계: S3에서 착장 검색
                sendSseMessage(emitter, "status", "S3에서 착장 검색 중...", 2);
                
                // 3단계: 외부 API 호출
                sendSseMessage(emitter, "status", "외부 API 호출 중...", 3);
                
                // 외부 API 호출 (localhost:6020)
                String externalApiUrl = llmExpertStreamUrl;
                
                // 요청 데이터 준비 (FastAPI 요구사항에 맞춤)
                Map<String, Object> expertRequest = new HashMap<>();
                expertRequest.put("user_input", userInput);
                expertRequest.put("expert_type", expertType);
                expertRequest.put("room_id", roomId);
                
                // user_profile이 있는 경우에만 포함
                if (userProfile != null && !userProfile.trim().isEmpty()) {
                    Map<String, Object> userProfileMap = new HashMap<>();
                    userProfileMap.put("profile_text", userProfile);
                    userProfileMap.put("age", "20대");
                    userProfileMap.put("gender", "남성");
                    userProfileMap.put("style", "미니멀한 스타일 선호");
                    userProfileMap.put("preference", "편안함 중시");
                    expertRequest.put("user_profile", userProfileMap);
                }
                
                expertRequest.put("context_info", null);
                expertRequest.put("json_data", null);
                
                System.out.println("🌐 외부 API 호출: " + externalApiUrl);
                System.out.println("📋 요청 데이터: " + expertRequest);
                
                // HTTP 헤더 설정
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(expertRequest, headers);
                
                try {
                    // 외부 API 호출
                    ResponseEntity<String> response = restTemplate.exchange(
                        externalApiUrl,
                        HttpMethod.POST,
                        entity,
                        String.class
                    );
                    
                    if (response.getStatusCode() == HttpStatus.OK) {
                        System.out.println("✅ 외부 API 응답 성공");
                        System.out.println("📥 응답 내용: " + response.getBody());
                        
                        // 외부 API 응답을 파싱하여 청크별로 SSE 전송
                        String responseBody = response.getBody();
                        if (responseBody != null && responseBody.contains("data:")) {
                            // data: 라인들을 분리하여 처리
                            String[] lines = responseBody.split("\n");
                            
                            for (String line : lines) {
                                if (line.startsWith("data:")) {
                                    String jsonData = line.substring(5).trim(); // "data: " 제거
                                    if (!jsonData.isEmpty()) {
                                        try {
                                            // JSON 파싱
                                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                            Map<String, Object> parsedData = mapper.readValue(jsonData, Map.class);
                                            
                                            // content 타입이고 chunk가 있는 경우만 전송 (status 타입 제외)
                                            if ("content".equals(parsedData.get("type")) && parsedData.containsKey("chunk")) {
                                                String chunk = (String) parsedData.get("chunk");
                                                // 각 청크를 개별적으로 SSE로 전송
                                                sendSseMessage(emitter, "content", chunk, 4);
                                                // 청크 간 약간의 딜레이 (스트리밍 효과)
                                                Thread.sleep(100);
                                            }
                                            // status 타입이나 다른 타입의 메시지는 전송하지 않음
                                        } catch (Exception e) {
                                            // JSON 파싱 실패 시 무시
                                            continue;
                                        }
                                    }
                                }
                            }
                        } else {
                            // 일반 응답인 경우
                            sendSseMessage(emitter, "content", "외부 API 응답: " + responseBody, 4);
                        }
                    } else {
                        System.out.println("❌ 외부 API 응답 실패: " + response.getStatusCode());
                        sendSseMessage(emitter, "error", "외부 API 호출 실패: " + response.getStatusCode(), 4);
                    }
                    
                } catch (Exception e) {
                    System.out.println("💥 외부 API 호출 중 오류 발생: " + e.getMessage());
                    e.printStackTrace();
                    sendSseMessage(emitter, "error", "외부 API 호출 오류: " + e.getMessage(), 4);
                }
                
                // 4단계: 완료
                sendSseMessage(emitter, "status", "테스트 완료", 5);
                
                // 최종 완료 메시지
                Map<String, Object> finalData = new HashMap<>();
                finalData.put("message", "SSE 테스트 완료");
                finalData.put("timestamp", System.currentTimeMillis());
                finalData.put("source", "sse_test_controller");
                
                sendSseMessage(emitter, "complete", finalData, 6);
                
                System.out.println("🎉 SSE 테스트 완료");
                
            } catch (Exception e) {
                System.out.println("💥 SSE 테스트 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
                try {
                    sendSseMessage(emitter, "error", "테스트 오류: " + e.getMessage(), -1);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            } finally {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        return emitter;
    }
    
    private void sendSseMessage(SseEmitter emitter, String type, Object data, int step) throws IOException {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("data", data);
        message.put("step", step);
        message.put("timestamp", System.currentTimeMillis());
        
        String jsonMessage;
        try {
            jsonMessage = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message);
        } catch (Exception e) {
            jsonMessage = "{\"type\":\"error\",\"data\":\"JSON 변환 실패\",\"step\":" + step + "}";
        }
        
        // 각 메시지 타입에 맞는 이벤트 이름 사용
        emitter.send(SseEmitter.event()
            .data(jsonMessage)
            .id(String.valueOf(step))
            .name(type));  // "message" 대신 type 사용 (status, content, complete, error)
        
        System.out.println("📤 SSE 메시지 전송: " + jsonMessage);
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "SSE Test Controller is running");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }
}
