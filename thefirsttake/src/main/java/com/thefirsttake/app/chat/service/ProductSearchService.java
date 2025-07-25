package com.thefirsttake.app.chat.service;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ProductSearchService {
    private final RestTemplate restTemplate;
    
    @Value("${ai.server.host}")
    private String aiServerHost;
    
    @Value("${ai.server.port}")
    private String aiServerPort;
    
    private String getSearchApiUrl() {
        return String.format("http://%s:%s/api/v1/search/", aiServerHost, aiServerPort);
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
     * 상품 검색 결과에서 이미지 URL 추출
     * @param searchResult 상품 검색 결과
     * @return 이미지 URL (없으면 null)
     */
    public String extractProductImageUrl(Map<String, Object> searchResult) {
        try {
            if (searchResult == null || !(Boolean) searchResult.getOrDefault("success", false)) {
                log.warn("상품 검색 결과가 유효하지 않습니다.");
                return null;
            }
            
            // data 필드에서 이미지 URL 추출 시도
            Object data = searchResult.get("data");
            if (data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                
                // 다양한 가능한 필드명으로 이미지 URL 찾기
                String imageUrl = findImageUrlInMap(dataMap);
                if (imageUrl != null) {
                    log.info("상품 이미지 URL 추출 성공: {}", imageUrl);
                    return imageUrl;
                }
            }
            
            log.warn("상품 이미지 URL을 찾을 수 없습니다.");
            return null;
            
        } catch (Exception e) {
            log.error("상품 이미지 URL 추출 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Map에서 이미지 URL 찾기
     */
    private String findImageUrlInMap(Map<String, Object> dataMap) {
        // 가능한 이미지 URL 필드명들
        String[] possibleImageFields = {
            "image_url", "imageUrl", "product_image", "productImage", 
            "image", "url", "thumbnail", "photo", "picture"
        };
        
        for (String field : possibleImageFields) {
            Object value = dataMap.get(field);
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return (String) value;
            }
        }
        
        // 중첩된 객체에서도 찾기
        for (Object value : dataMap.values()) {
            if (value instanceof Map) {
                String nestedUrl = findImageUrlInMap((Map<String, Object>) value);
                if (nestedUrl != null) {
                    return nestedUrl;
                }
            }
        }
        
        return null;
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