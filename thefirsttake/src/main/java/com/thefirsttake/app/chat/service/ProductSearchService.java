package com.thefirsttake.app.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 상품 검색 API 호출 전담 서비스
 * - 에이전트 응답 메시지를 바탕으로 상품 검색
 * - 외부 상품 검색 API와의 통신
 */
@Service
@Slf4j
public class ProductSearchService {
    private final RestTemplate restTemplate;
    
    public ProductSearchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Value("${ai.server.host}")
    private String aiServerHost;
    
    @Value("${ai.server.port}")
    private String aiServerPort;
    
    private String getSearchApiUrl() {
        return String.format("http://%s:%s/search/", aiServerHost, aiServerPort);
    }

    /**
     * 에이전트 응답 메시지를 바탕으로 상품 검색 API 호출
     * @param message 에이전트 응답 메시지
     * @return 검색 결과
     */
    public Map<String, Object> searchProducts(String message) {
        try {
            log.info("상품 검색 API 호출 시작 - 메시지: {}", message);
            
            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 요청 바디 설정
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("messages", message);
            
            // HTTP 엔티티 생성
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            // API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(getSearchApiUrl(), request, Map.class);
            
            // 응답 처리
            Map<String, Object> responseBody = response.getBody();
            return responseBody;
            // if (responseBody != null) {
            //     log.info("상품 검색 API 응답 성공: {}", responseBody);
            //     return responseBody;
            // } else {
            //     log.warn("상품 검색 API 응답이 null입니다.");
            //     return createErrorResponse("API 응답이 null입니다.");
            // }
            
        } catch (Exception e) {
            log.error("상품 검색 API 호출 실패: {}", e.getMessage(), e);
            return createErrorResponse("상품 검색 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 상품 검색 결과에서 모든 이미지 URL 추출
     * 새로운 응답 형식: {"success": true, "data": {"data": [{"image_url": "..."}, ...]}}
     * @param searchResult 상품 검색 결과
     * @return 이미지 URL 리스트 (없으면 빈 리스트)
     */
    public java.util.List<String> extractProductImageUrls(Map<String, Object> searchResult) {
        java.util.List<String> imageUrls = new java.util.ArrayList<>();
        
        try {
            if (searchResult == null || !(Boolean) searchResult.getOrDefault("success", false)) {
                log.warn("상품 검색 결과가 유효하지 않습니다.");
                return imageUrls;
            }
            
            // data.data 배열에서 모든 항목의 image_url 추출
            Object data = searchResult.get("data");
            if (data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                Object dataArray = dataMap.get("data");
                
                if (dataArray instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) dataArray;
                    
                    for (Object item : list) {
                        if (item instanceof Map) {
                            Map<String, Object> itemMap = (Map<String, Object>) item;
                            String imageUrl = (String) itemMap.get("image_url");
                            
                            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                                imageUrls.add(imageUrl);
                            }
                        }
                    }
                }
            }
            
            if (!imageUrls.isEmpty()) {
                log.info("상품 이미지 URL {}개 추출 성공: {}", imageUrls.size(), imageUrls);
            } else {
                log.warn("상품 이미지 URL을 찾을 수 없습니다.");
            }
            
            return imageUrls;
            
        } catch (Exception e) {
            log.error("상품 이미지 URL 추출 중 오류: {}", e.getMessage(), e);
            return imageUrls;
        }
    }
    

    
    /**
     * 오류 응답 생성
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", errorMessage);
        errorResponse.put("data", new HashMap<>());
        return errorResponse;
    }
} 