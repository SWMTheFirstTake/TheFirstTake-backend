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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Flux;

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
    private final WebClient.Builder webClientBuilder;
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
            
            // LLM API 진짜 스트림 호출
            processLlmStreamResponse(expertRequest, expertType, emitter, cancelled, finalText);
            
            // 상품 검색 및 캐싱
            if (!cancelled.get()) {
                products = productSearchStreamService.searchAndCacheProducts(finalText.toString());
                
                // AI 응답 저장
                if (!products.isEmpty()) {
                    messageStorageService.saveAIResponse(sessionId, expertType, finalText.toString(), products, roomId);
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
     * LLM API 진짜 스트림 호출 및 처리
     */
    private void processLlmStreamResponse(Map<String, Object> expertRequest, String expertType, 
                                        SseEmitter emitter, AtomicBoolean cancelled, StringBuilder finalText) {
        try {
            // LLM API 호출 메트릭 시작
            var timerSample = streamMetricsService.startLlmApiCall(expertType);
            
            // WebClient로 진짜 스트림 호출
            webClientBuilder.build().post()
                .uri(llmExpertStreamUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(expertRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> {
                    if (cancelled.get()) return;
                    
                    // 디버깅: 모든 청크 로그 출력
                    log.info("Received chunk from LLM server: expertType={}, chunk={}", expertType, chunk);
                    
                    // JSON 형식 파싱 (data: 접두사 없음)
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsed = OBJECT_MAPPER.readValue(chunk, Map.class);
                        String type = String.valueOf(parsed.get("type"));
                        
                        // content 이벤트만 처리
                        if ("content".equals(type) && parsed.containsKey("chunk")) {
                            String contentChunk = String.valueOf(parsed.get("chunk"));
                            finalText.append(contentChunk);
                            
                            log.info("Processing and sending content event: expertType={}, chunk={}", expertType, contentChunk);
                            
                            // 즉시 클라이언트로 전송 (딜레이 없음!)
                            sseConnectionService.sendContentEvent(emitter, contentChunk, expertType, getAgentName(expertType));
                        }
                        
                    } catch (Exception e) {
                        log.warn("Stream chunk parsing error: expertType={}, chunk={}, error={}", expertType, chunk, e.getMessage());
                    }
                })
                .doOnError(error -> {
                    log.error("LLM 스트림 호출 실패: expertType={}, error={}", expertType, error.getMessage(), error);
                    if (!cancelled.get()) {
                        sseConnectionService.sendErrorEvent(emitter, "LLM 스트림 호출 실패: " + error.getMessage(), expertType);
                    }
                    
                    // 메트릭 종료 (에러)
                    streamMetricsService.endLlmApiCall(timerSample, expertType, 500, null, false);
                })
                .doOnComplete(() -> {
                    log.info("LLM 스트림 완료: expertType={}", expertType);
                    
                    // 메트릭 종료 (성공)
                    streamMetricsService.endLlmApiCall(timerSample, expertType, 200, finalText.toString(), true);
                })
                .blockLast(); // 스트림 완료까지 대기
                
        } catch (Exception e) {
            log.error("LLM 스트림 처리 중 오류: expertType={}, error={}", expertType, e.getMessage(), e);
            if (!cancelled.get()) {
                sseConnectionService.sendErrorEvent(emitter, "LLM 스트림 처리 오류: " + e.getMessage(), expertType);
            }
        }
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

