package com.thefirsttake.app.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.client.RestTemplate;

/**
 * 새로운 LLM 서버 연결을 담당하는 서비스
 * - 새로운 LLM 서버 스트림 처리
 * - token, status, message, [DONE] 이벤트 처리
 * - 상품 검색 API 연동
 */
@Service
@Slf4j
public class NewLLMStreamService {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Value("${llm.server.new-stream-url:https://the-first-take.com/langgraph/llm_search/stream}")
    private String newLlmStreamUrl;
    
    private final WebClient.Builder webClientBuilder;
    private final SSEConnectionService sseConnectionService;
    private final MessageStorageService messageStorageService;
    private final RestTemplate restTemplate;
    private final ProductSearchService productSearchService;
    private final RedisTemplate<String, String> redisTemplate;
    
    public NewLLMStreamService(WebClient.Builder webClientBuilder,
                              SSEConnectionService sseConnectionService,
                              MessageStorageService messageStorageService,
                              RestTemplate restTemplate,
                              ProductSearchService productSearchService,
                              @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.webClientBuilder = webClientBuilder;
        this.sseConnectionService = sseConnectionService;
        this.messageStorageService = messageStorageService;
        this.restTemplate = restTemplate;
        this.productSearchService = productSearchService;
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 새로운 LLM 서버로 스트림 요청 - 새로운 응답 형태에 맞게 처리
     */
    public ExpertProcessResult processNewLlmStream(String userInput, String userProfile, String roomId, 
                                                  String sessionId, SseEmitter emitter, AtomicBoolean cancelled) {
        
        StringBuilder finalText = new StringBuilder();
        List<com.thefirsttake.app.chat.dto.response.ProductInfo> products = new ArrayList<>();
        
        try {
            // 전문가별 텍스트와 상품 정보를 저장할 맵
            Map<String, StringBuilder> expertTexts = new HashMap<>();
            Map<String, List<com.thefirsttake.app.chat.dto.response.ProductInfo>> expertProducts = new HashMap<>();
            Map<String, Boolean> expertCompleted = new HashMap<>();
            
            // 전문가 리스트 초기화 (고정 순서: color_expert -> style_analyst -> fitting_coordinator)
            List<String> expertList = getExpertList();
            for (String expert : expertList) {
                expertTexts.put(expert, new StringBuilder());
                expertProducts.put(expert, new ArrayList<>());
                expertCompleted.put(expert, false);
            }
            
            // 현재 활성 전문가 (순차적으로 변경)
            AtomicInteger currentExpertIndex = new AtomicInteger(0); // 0: color_expert, 1: style_analyst, 2: fitting_coordinator
            AtomicInteger completedExpertCount = new AtomicInteger(0); // 완료된 전문가 수
            
            // 요청 데이터 준비
            Map<String, Object> requestData = prepareNewLlmRequest(userInput, userProfile, roomId);
            
            if (cancelled.get()) {
                return new ExpertProcessResult("", products, false);
            }
            
            log.info("새로운 LLM 서버 스트림 호출 시작: roomId={}", roomId);
            
            // WebClient로 스트림 호출
            webClientBuilder.build().post()
                .uri(newLlmStreamUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestData)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> {
                    if (cancelled.get()) return;
                    
                    try {
                        processStreamChunk(chunk, emitter, expertTexts, expertProducts, roomId, sessionId, 
                                         expertCompleted, currentExpertIndex, completedExpertCount);
                    } catch (Exception e) {
                        log.warn("스트림 청크 처리 오류: chunk={}, error={}", chunk, e.getMessage());
                    }
                })
                .doOnError(error -> {
                    log.error("새로운 LLM 스트림 호출 실패: error={}", error.getMessage(), error);
                    if (!cancelled.get()) {
                        sseConnectionService.sendErrorEvent(emitter, "새로운 LLM 스트림 호출 실패: " + error.getMessage(), null);
                    }
                })
                .doOnComplete(() -> {
                    log.info("새로운 LLM 스트림 완료: roomId={}", roomId);
                })
                .blockLast(); // 스트림 완료까지 대기
            
            // 모든 전문가 결과를 누적
            for (String expertType : expertList) {
                if (cancelled.get()) break;
                
                StringBuilder expertText = expertTexts.get(expertType);
                List<com.thefirsttake.app.chat.dto.response.ProductInfo> expertProductList = expertProducts.get(expertType);
                
                if (expertText.length() > 0) {
                    finalText.append(expertText.toString());
                    products.addAll(expertProductList);
                    
                    log.info("✅ 새로운 LLM 전문가 결과 누적: expert={}, textLength={}, productsCount={}", 
                            expertType, expertText.length(), expertProductList.size());
                }
            }
                
        } catch (Exception e) {
            log.error("새로운 LLM 스트림 처리 중 오류: roomId={}, error={}", roomId, e.getMessage(), e);
            if (!cancelled.get()) {
                sseConnectionService.sendErrorEvent(emitter, "새로운 LLM 스트림 처리 오류: " + e.getMessage(), null);
            }
        }
        
        return new ExpertProcessResult(finalText.toString(), products, true);
    }
    
    /**
     * 전문가 처리 결과 클래스 (ExpertStreamService와 동일)
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
    
    /**
     * 전문가 리스트 반환 (고정 순서: color_expert -> style_analyst -> fitting_coordinator)
     */
    public List<String> getExpertList() {
        List<String> expertList = new ArrayList<>();
        expertList.add("color_expert");
        expertList.add("style_analyst");
        expertList.add("fitting_coordinator");
        return expertList;
    }
    
    
    /**
     * 전문가 완료 이벤트 전송 (기존 로직과 동일)
     */
    private void sendExpertCompleteEvent(SseEmitter emitter, String message, String expertType, 
                                       List<com.thefirsttake.app.chat.dto.response.ProductInfo> products) {
        try {
            String agentName = getAgentName(expertType);
            
            // 상품 정보 상세 로그
            if (products != null && !products.isEmpty()) {
                for (int i = 0; i < products.size(); i++) {
                    com.thefirsttake.app.chat.dto.response.ProductInfo product = products.get(i);
                    log.info("상품 정보 전송: index={}, productId={}, productUrl={}", 
                            i, product.getProductId(), product.getProductUrl());
                }
            } else {
                log.warn("⚠️ 상품 정보가 없습니다: expert={}, products={}", expertType, products);
            }
            
            sseConnectionService.sendCompleteEvent(emitter, message, expertType, agentName, products);
            log.info("✅ 전문가 완료 이벤트 전송 성공: expert={}, messageLength={}, productsCount={}", 
                    expertType, message.length(), products != null ? products.size() : 0);
        } catch (Exception e) {
            log.error("❌ 전문가 완료 이벤트 전송 실패: expert={}, error={}", expertType, e.getMessage(), e);
        }
    }
    
    /**
     * 새로운 LLM 서버 요청 데이터 준비
     */
    private Map<String, Object> prepareNewLlmRequest(String userInput, String userProfile, String roomId) {
        Map<String, Object> requestData = new HashMap<>();
        
        // agent_config 설정
        Map<String, Object> agentConfig = new HashMap<>();
        agentConfig.put("spicy_level", 0.8);
        requestData.put("agent_config", agentConfig);
        
        // 메시지 설정
        requestData.put("message", userInput);
        
        // 모델 설정
        requestData.put("model", "gemini-2.0-flash-lite");
        
        // 스트림 토큰 활성화
        requestData.put("stream_tokens", true);
        
        // 스레드 ID 및 사용자 ID (임시로 roomId 사용)
        requestData.put("thread_id", roomId);
        requestData.put("user_id", roomId);
        
        return requestData;
    }
    
    /**
     * 스트림 청크 처리 - 새로운 응답 형태에 맞게 처리
     */
    private void processStreamChunk(String chunk, SseEmitter emitter, 
                                  Map<String, StringBuilder> expertTexts,
                                  Map<String, List<com.thefirsttake.app.chat.dto.response.ProductInfo>> expertProducts,
                                  String roomId, String sessionId,
                                  Map<String, Boolean> expertCompleted,
                                  AtomicInteger currentExpertIndex,
                                  AtomicInteger completedExpertCount) {
        
        // data: 접두사 제거
        if (chunk.startsWith("data: ")) {
            chunk = chunk.substring(6);
        }
        
        // [DONE] 체크
        if ("[DONE]".equals(chunk.trim())) {
            log.info("새로운 LLM 스트림 완료 신호 수신: roomId={}", roomId);
            return;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(chunk, Map.class);
            String type = String.valueOf(parsed.get("type"));
            
            switch (type) {
                case "token":
                    processTokenEvent(parsed, emitter, expertTexts, currentExpertIndex, completedExpertCount);
                    break;
                case "status":
                    // status 이벤트는 무시 (처리할 필요 없음)
                    log.debug("status 이벤트 무시: chunk={}", chunk);
                    break;
                case "message":
                    processMessageEvent(parsed, emitter, expertProducts, roomId, sessionId, expertTexts, expertCompleted, currentExpertIndex, completedExpertCount);
                    break;
                default:
                    log.debug("알 수 없는 이벤트 타입: type={}, chunk={}", type, chunk);
            }
            
        } catch (Exception e) {
            log.warn("스트림 청크 파싱 오류: chunk={}, error={}", chunk, e.getMessage());
        }
    }
    
    /**
     * token 이벤트 처리 - 받는대로 즉시 사용자에게 전송
     */
    private void processTokenEvent(Map<String, Object> parsed, SseEmitter emitter, 
                                 Map<String, StringBuilder> expertTexts,
                                 AtomicInteger currentExpertIndex,
                                 AtomicInteger completedExpertCount) {
        String content = String.valueOf(parsed.get("content"));
        if (content != null && !content.equals("null")) {
            // 현재 활성 전문가 결정 (완료된 전문가 수를 기준으로)
            List<String> expertList = getExpertList();
            int activeExpertIndex = completedExpertCount.get();
            
            // 모든 전문가가 완료되었으면 마지막 전문가 사용
            if (activeExpertIndex >= expertList.size()) {
                activeExpertIndex = expertList.size() - 1;
            }
            
            String currentExpert = expertList.get(activeExpertIndex);
            
            // 해당 전문가의 텍스트에 추가
            expertTexts.get(currentExpert).append(content);
            
            String agentName = getAgentName(currentExpert);
            
            // 클라이언트로 즉시 전송
            sseConnectionService.sendContentEvent(emitter, content, currentExpert, agentName);
            
            log.debug("token 이벤트 처리: expert={}, completedCount={}, content={}", currentExpert, completedExpertCount.get(), content);
        }
    }
    
    
    /**
     * message 이벤트 처리 - 상품 정보 추출 및 검색 후 complete 이벤트 전송
     */
    private void processMessageEvent(Map<String, Object> parsed, SseEmitter emitter,
                                   Map<String, List<com.thefirsttake.app.chat.dto.response.ProductInfo>> expertProducts,
                                   String roomId, String sessionId,
                                   Map<String, StringBuilder> expertTexts,
                                   Map<String, Boolean> expertCompleted,
                                   AtomicInteger currentExpertIndex,
                                   AtomicInteger completedExpertCount) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) parsed.get("content");
            if (content == null) return;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> additionalKwargs = (Map<String, Object>) content.get("additional_kwargs");
            if (additionalKwargs == null) return;
            
            String type = String.valueOf(additionalKwargs.get("type"));
            if (!"refer".equals(type)) return;
            
            @SuppressWarnings("unchecked")
            List<String> productIds = (List<String>) additionalKwargs.get("product_ids");
            if (productIds == null || productIds.isEmpty()) return;
            
            log.info("상품 ID 발견: productIds={}", productIds);
            
            // 현재 활성 전문가 결정 (순차적으로)
            List<String> expertList = getExpertList();
            int activeExpertIndex = completedExpertCount.get();
            
            // 모든 전문가가 완료되었으면 마지막 전문가 사용
            if (activeExpertIndex >= expertList.size()) {
                activeExpertIndex = expertList.size() - 1;
            }
            
            String expertType = expertList.get(activeExpertIndex);
            
            log.info("상품 정보를 전문가에게 할당: expertType={}, productIds={}, completedCount={}, activeIndex={}", 
                    expertType, productIds, completedExpertCount.get(), activeExpertIndex);
            
            // 각 상품 ID에 대해 검색 API 호출
            List<com.thefirsttake.app.chat.dto.response.ProductInfo> newProducts = new ArrayList<>();
            for (String productId : productIds) {
                try {
                    com.thefirsttake.app.chat.dto.response.ProductInfo productInfo = searchProductById(productId);
                    if (productInfo != null) {
                        expertProducts.get(expertType).add(productInfo);
                        newProducts.add(productInfo);
                        log.info("상품 정보 추가: expert={}, productId={}, imageUrl={}", 
                                expertType, productId, productInfo.getProductUrl());
                    }
                } catch (Exception e) {
                    log.error("상품 검색 실패: productId={}, error={}", productId, e.getMessage());
                }
            }
            
            // message 이벤트가 오면 해당 전문가가 완료된 것으로 간주
            try {
                StringBuilder expertText = expertTexts.get(expertType);
                if (expertText != null && expertText.length() > 0) {
                    // 전문가 완료 이벤트 전송 (상품 정보 포함)
                    sendExpertCompleteEvent(emitter, expertText.toString(), expertType, expertProducts.get(expertType));
                    
                    // 캐시 저장
                    messageStorageService.saveAIResponseToCache(sessionId, expertType, expertText.toString(), expertProducts.get(expertType), roomId);
                    
                    expertCompleted.put(expertType, true);
                    
                    // 완료된 전문가 수 증가
                    completedExpertCount.incrementAndGet();
                    
                    log.info("✅ 전문가 완료 이벤트 전송: expert={}, completedCount={}, productsCount={}", 
                            expertType, completedExpertCount.get(), expertProducts.get(expertType).size());
                }
            } catch (Exception e) {
                log.error("전문가 완료 이벤트 전송 실패: expert={}, error={}", expertType, e.getMessage(), e);
            }
            
        } catch (Exception e) {
            log.error("message 이벤트 처리 오류: error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 상품 ID로 상품 정보 검색 및 Redis 저장
     */
    private com.thefirsttake.app.chat.dto.response.ProductInfo searchProductById(String productId) {
        try {
            log.info("상품 검색 시작: productId={}", productId);
            
            // ProductSearchService를 사용하여 상품 검색
            String searchUrl = "https://the-first-take.com/search/" + productId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(searchUrl, Map.class);
            
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                
                if (data != null) {
                    String productIdValue = String.valueOf(data.get("product_id"));
                    String imageUrl = String.valueOf(data.get("image_url"));
                    
                    if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
                        // Redis에 저장 (가상피팅용)
                        String redisKey = "product_url_" + productId.trim();
                        redisTemplate.opsForValue().set(redisKey, imageUrl.trim(), 36000, java.util.concurrent.TimeUnit.SECONDS);
                        
                        com.thefirsttake.app.chat.dto.response.ProductInfo productInfo = com.thefirsttake.app.chat.dto.response.ProductInfo.builder()
                            .productId(productIdValue)
                            .productUrl(imageUrl)
                            .build();
                        
                        log.info("✅ 상품 정보 검색 및 Redis 저장 성공: productId={}, productUrl={}", productIdValue, imageUrl);
                        return productInfo;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("상품 검색 중 오류 발생: productId={}, error={}", productId, e.getMessage(), e);
        }
        
        return null;
    }
    
    
    /**
     * 전문가 타입에 따른 이름 반환
     */
    private String getAgentName(String expertType) {
        return switch (expertType) {
            case "style_analyst" -> "스타일 분석가";
            case "color_expert" -> "컬러 전문가";
            case "fitting_coordinator" -> "피팅 코디네이터";
            default -> expertType;
        };
    }
}
