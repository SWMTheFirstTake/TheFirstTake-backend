package com.thefirsttake.app.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 상품 정보 Redis 캐싱 서비스
 * - AI 서버에서 받은 상품 정보를 Redis에 캐싱
 * - product_id:{product_id} 키로 상품 정보 저장
 */
@Service
@Slf4j
public class ProductCacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public ProductCacheService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
                              ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * AI 서버 응답에서 상품 정보를 추출하여 Redis에 캐싱
     * @param searchResult AI 서버의 전체 응답
     */
    public void cacheProductsFromSearchResult(Map<String, Object> searchResult) {
        if (searchResult == null || !searchResult.containsKey("data")) {
            log.warn("검색 결과가 null이거나 data 필드가 없습니다.");
            return;
        }
        
        try {
            Map<String, Object> data = (Map<String, Object>) searchResult.get("data");
            List<Map<String, Object>> productList = (List<Map<String, Object>>) data.get("data");
            
            if (productList == null || productList.isEmpty()) {
                log.info("캐싱할 상품이 없습니다.");
                return;
            }
            
            int cachedCount = 0;
            for (Map<String, Object> item : productList) {
                if (cacheProductInfo(item)) {
                    cachedCount++;
                }
            }
            
            log.info("✅ 상품 정보 캐싱 완료: 총 {}개 중 {}개 새로 캐싱됨", productList.size(), cachedCount);
            
        } catch (Exception e) {
            log.error("❌ 상품 정보 캐싱 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 개별 상품 정보를 Redis에 캐싱
     * @param item AI 서버 응답의 개별 상품 항목
     * @return 새로 캐싱되었는지 여부 (이미 존재하면 false)
     */
    private boolean cacheProductInfo(Map<String, Object> item) {
        try {
            // product_id 추출
            String productId = extractProductId(item);
            if (productId == null) {
                log.warn("product_id를 찾을 수 없습니다: {}", item);
                return false;
            }
            
            String cacheKey = "product_id:" + productId;
            
            // 이미 캐시에 존재하는지 확인
            if (redisTemplate.hasKey(cacheKey)) {
                log.debug("상품 정보가 이미 캐시에 존재합니다: {}", productId);
                return false;
            }
            
            // 캐싱할 상품 정보 추출
            Map<String, Object> productInfo = extractProductInfo(item);
            if (productInfo.isEmpty()) {
                log.warn("캐싱할 상품 정보가 없습니다: {}", productId);
                return false;
            }
            
            // Redis에 저장
            String productInfoJson = objectMapper.writeValueAsString(productInfo);
            redisTemplate.opsForValue().set(cacheKey, productInfoJson);
            
            log.debug("✅ 상품 정보 캐싱: {} -> {}", productId, productInfo);
            return true;
            
        } catch (Exception e) {
            log.error("❌ 개별 상품 캐싱 실패: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * AI 서버 응답에서 product_id 추출
     */
    private String extractProductId(Map<String, Object> item) {
        // _id 필드에서 추출
        if (item.containsKey("_id")) {
            return (String) item.get("_id");
        }
        
        // products.product_id에서 추출
        if (item.containsKey("products")) {
            Map<String, Object> products = (Map<String, Object>) item.get("products");
            if (products != null && products.containsKey("product_id")) {
                return (String) products.get("product_id");
            }
        }
        
        return null;
    }
    
    /**
     * 캐싱할 상품 정보 추출
     * - product_name
     * - comprehensive_description  
     * - style_tags
     * - tpo_tags
     */
    private Map<String, Object> extractProductInfo(Map<String, Object> item) {
        Map<String, Object> productInfo = new HashMap<>();
        
        try {
            // products 섹션에서 추출
            if (item.containsKey("products")) {
                Map<String, Object> products = (Map<String, Object>) item.get("products");
                if (products != null) {
                    // product_name 추출
                    if (products.containsKey("product_name")) {
                        productInfo.put("product_name", products.get("product_name"));
                    }
                    
                    // comprehensive_description 추출 (captions > comprehensive_description)
                    if (products.containsKey("captions")) {
                        Map<String, Object> captions = (Map<String, Object>) products.get("captions");
                        if (captions != null && captions.containsKey("comprehensive_description")) {
                            productInfo.put("comprehensive_description", captions.get("comprehensive_description"));
                        }
                    }
                }
            }
            
            // product_skus 섹션에서 추출
            if (item.containsKey("product_skus")) {
                Map<String, Object> productSkus = (Map<String, Object>) item.get("product_skus");
                if (productSkus != null) {
                    // style_tags 추출
                    if (productSkus.containsKey("style_tags")) {
                        productInfo.put("style_tags", productSkus.get("style_tags"));
                    }
                    
                    // tpo_tags 추출
                    if (productSkus.containsKey("tpo_tags")) {
                        productInfo.put("tpo_tags", productSkus.get("tpo_tags"));
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("❌ 상품 정보 추출 중 오류: {}", e.getMessage(), e);
        }
        
        return productInfo;
    }
    
    /**
     * Redis에서 상품 정보 조회
     * @param productId 상품 ID
     * @return 상품 정보 Map (없으면 null)
     */
    public Map<String, Object> getProductInfo(String productId) {
        try {
            String cacheKey = "product_id:" + productId;
            String productInfoJson = redisTemplate.opsForValue().get(cacheKey);
            
            if (productInfoJson != null) {
                return objectMapper.readValue(productInfoJson, Map.class);
            }
            
        } catch (JsonProcessingException e) {
            log.error("❌ 상품 정보 역직렬화 실패: productId={}, error={}", productId, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * AI 서버 응답에서 상품 ID 목록 추출
     * @param searchResult AI 서버의 전체 응답
     * @return 상품 ID 리스트
     */
    public List<String> extractProductIds(Map<String, Object> searchResult) {
        List<String> productIds = new ArrayList<>();
        
        if (searchResult == null || !searchResult.containsKey("data")) {
            log.warn("검색 결과가 null이거나 data 필드가 없습니다.");
            return productIds;
        }
        
        try {
            Map<String, Object> data = (Map<String, Object>) searchResult.get("data");
            List<Map<String, Object>> productList = (List<Map<String, Object>>) data.get("data");
            
            if (productList == null || productList.isEmpty()) {
                log.info("추출할 상품 ID가 없습니다.");
                return productIds;
            }
            
            for (Map<String, Object> item : productList) {
                String productId = extractProductId(item);
                if (productId != null) {
                    productIds.add(productId);
                }
            }
            
            log.info("✅ 상품 ID 추출 완료: {}개", productIds.size());
            
        } catch (Exception e) {
            log.error("❌ 상품 ID 추출 중 오류 발생: {}", e.getMessage(), e);
        }
        
        return productIds;
    }
    
    /**
     * 특정 상품의 캐시 삭제
     * @param productId 상품 ID
     * @return 삭제 성공 여부
     */
    public boolean deleteProductCache(String productId) {
        String cacheKey = "product_id:" + productId;
        return Boolean.TRUE.equals(redisTemplate.delete(cacheKey));
    }
}
