package com.thefirsttake.app.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 전문가별 스트림 처리를 담당하는 서비스
 * - 전문가 리스트 관리
 * - 각 전문가별 API 호출
 * - 스트림 응답 파싱 및 전송
 * - 전문가 완료 상태 추적
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpertStreamService {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Value("${llm.server.expert-stream-url}")
    private String llmExpertStreamUrl;
    
    private final RestTemplate restTemplate;
    private final SSEConnectionService sseConnectionService;
    private final StreamMetricsService streamMetricsService;
    private final ProductSearchStreamService productSearchStreamService;
    private final MessageStorageService messageStorageService;
    
    /**
     * 전문가 리스트 반환
     * @return 전문가 리스트
     */
    public List<String> getExpertList() {
        List<String> expertList = new ArrayList<>();
        expertList.add("style_analyst");
        expertList.add("color_expert");
        expertList.add("fitting_coordinator");
        return expertList;
    }
    
    /**
     * 전문가 완료 상태 추적 Map 생성
     * @param expertList 전문가 리스트
     * @return 전문가 완료 상태 Map
     */
    public Map<String, Boolean> createExpertCompletedMap(List<String> expertList) {
        Map<String, Boolean> expertCompleted = new ConcurrentHashMap<>();
        for (String expert : expertList) {
            expertCompleted.put(expert, false);
        }
        return expertCompleted;
    }
    
    /**
     * 단일 전문가 처리
     * @param expertType 전문가 타입
     * @param userInput 사용자 입력
     * @param userProfile 사용자 프로필
     * @param roomId 방 ID
     * @param sessionId 세션 ID
     * @param emitter SSE 에미터
     * @param cancelled 취소 상태
     * @return 처리 결과 (메시지, 상품 정보)
     */
    public ExpertProcessResult processExpert(String expertType, String userInput, String userProfile, 
                                           String roomId, String sessionId, SseEmitter emitter, 
                                           AtomicBoolean cancelled) {
        
        StringBuilder finalText = new StringBuilder();
        List<com.thefirsttake.app.chat.dto.response.ProductInfo> products = new ArrayList<>();
        
        try {
            // 전문가 처리 전 메모리 측정
            streamMetricsService.recordMemoryUsage();
            
            // 전문가 요청 데이터 준비
            Map<String, Object> expertRequest = prepareExpertRequest(expertType, userInput, userProfile, roomId);
            
            if (cancelled.get()) {
                return new ExpertProcessResult("", products, false);
            }
            
            // LLM API 호출
            ResponseEntity<String> response = callLlmApi(expertRequest, expertType);
            
            if (cancelled.get()) {
                return new ExpertProcessResult("", products, false);
            }
            
            // 응답 처리
            if (response.getStatusCode() == HttpStatus.OK) {
                finalText = processStreamResponse(response.getBody(), expertType, emitter, cancelled);
                
                // 상품 검색 및 캐싱
                if (!cancelled.get()) {
                    products = productSearchStreamService.searchAndCacheProducts(finalText.toString());
                    
                    // AI 응답 저장
                    if (!products.isEmpty()) {
                        messageStorageService.saveAIResponse(sessionId, expertType, finalText.toString(), products, roomId);
                    }
                }
                
            } else {
                log.error("LLM API 호출 실패: expertType={}, statusCode={}", expertType, response.getStatusCode());
                if (!cancelled.get()) {
                    sseConnectionService.sendErrorEvent(emitter, "외부 API 호출 실패: " + response.getStatusCode(), expertType);
                }
            }
            
        } catch (Exception e) {
            log.error("전문가 처리 중 오류: expertType={}, error={}", expertType, e.getMessage(), e);
            if (!cancelled.get()) {
                sseConnectionService.sendErrorEvent(emitter, "전문가 처리 오류: " + e.getMessage(), expertType);
            }
        }
        
        return new ExpertProcessResult(finalText.toString(), products, true);
    }
    
    /**
     * 전문가 요청 데이터 준비
     */
    private Map<String, Object> prepareExpertRequest(String expertType, String userInput, String userProfile, String roomId) {
        Map<String, Object> expertRequest = new HashMap<>();
        expertRequest.put("user_input", userInput);
        expertRequest.put("expert_type", expertType);
        expertRequest.put("room_id", roomId);
        
        // 사용자 프로필 포함 (있는 경우)
        if (userProfile != null && !userProfile.trim().isEmpty()) {
            Map<String, Object> userProfileMap = new HashMap<>();
            userProfileMap.put("profile_text", userProfile);
            expertRequest.put("user_profile", userProfileMap);
        }
        
        expertRequest.put("context_info", null);
        expertRequest.put("json_data", null);
        
        return expertRequest;
    }
    
    /**
     * LLM API 호출
     */
    private ResponseEntity<String> callLlmApi(Map<String, Object> expertRequest, String expertType) {
        // HTTP 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(expertRequest, headers);
        
        // LLM API 호출 메트릭 시작
        var timerSample = streamMetricsService.startLlmApiCall(expertType);
        
        // API 호출
        ResponseEntity<String> response = restTemplate.exchange(
                llmExpertStreamUrl,
                HttpMethod.POST,
                entity,
                String.class
        );
        
        // 메트릭 종료
        streamMetricsService.endLlmApiCall(
                timerSample, 
                expertType, 
                response.getStatusCode().value(), 
                response.getBody(), 
                response.getStatusCode() == HttpStatus.OK
        );
        
        return response;
    }
    
    /**
     * 스트림 응답 처리
     */
    private StringBuilder processStreamResponse(String body, String expertType, SseEmitter emitter, AtomicBoolean cancelled) {
        StringBuilder finalText = new StringBuilder();
        
        if (body != null && body.contains("data:")) {
            String[] lines = body.split("\n");
            for (String line : lines) {
                if (cancelled.get()) break;
                if (!line.startsWith("data:")) continue;
                
                String jsonData = line.substring(5).trim();
                if (jsonData.isEmpty()) continue;
                
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsed = OBJECT_MAPPER.readValue(jsonData, Map.class);
                    Object type = parsed.get("type");
                    
                    if ("content".equals(type) && parsed.containsKey("chunk")) {
                        String chunk = String.valueOf(parsed.get("chunk"));
                        finalText.append(chunk);
                        
                        // 청크를 즉시 전송
                        if (cancelled.get()) break;
                        sseConnectionService.sendContentEvent(emitter, chunk, expertType, getAgentName(expertType));
                        
                        try { 
                            Thread.sleep(100); // 스트림 딜레이
                        } catch (InterruptedException ie) { 
                            Thread.currentThread().interrupt(); 
                        }
                    }
                } catch (Exception e) {
                    log.warn("스트림 응답 파싱 오류: expertType={}, line={}, error={}", expertType, line, e.getMessage());
                }
            }
        }
        
        return finalText;
    }
    
    /**
     * 에이전트 이름 반환
     */
    private String getAgentName(String agentId) {
        return switch (agentId) {
            case "style_analyst" -> "스타일 분석가";
            case "color_expert" -> "컬러 전문가";
            case "fitting_coordinator" -> "피팅 코디네이터";
            default -> agentId;
        };
    }
    
    /**
     * 전문가 완료 이벤트 전송
     */
    public void sendExpertCompleteEvent(SseEmitter emitter, String message, String agentId, List<com.thefirsttake.app.chat.dto.response.ProductInfo> products) {
        sseConnectionService.sendCompleteEvent(emitter, message, agentId, getAgentName(agentId), products);
    }
    
    /**
     * 모든 전문가 완료 확인
     */
    public boolean areAllExpertsCompleted(Map<String, Boolean> expertCompleted) {
        return expertCompleted.values().stream().allMatch(Boolean::booleanValue);
    }
    
    /**
     * 전문가 완료 상태 업데이트
     */
    public void markExpertCompleted(Map<String, Boolean> expertCompleted, String expertType) {
        expertCompleted.put(expertType, true);
    }
    
    /**
     * 전문가 처리 결과 클래스
     */
    public static class ExpertProcessResult {
        private final String message;
        private final List<com.thefirsttake.app.chat.dto.response.ProductInfo> products;
        private final boolean success;
        
        public ExpertProcessResult(String message, List<com.thefirsttake.app.chat.dto.response.ProductInfo> products, boolean success) {
            this.message = message;
            this.products = products;
            this.success = success;
        }
        
        public String getMessage() { return message; }
        public List<com.thefirsttake.app.chat.dto.response.ProductInfo> getProducts() { return products; }
        public boolean isSuccess() { return success; }
    }
}
