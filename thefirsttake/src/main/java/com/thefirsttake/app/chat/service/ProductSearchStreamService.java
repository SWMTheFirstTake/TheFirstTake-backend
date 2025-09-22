package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.dto.response.ProductInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 스트림 처리 중 상품 검색 및 캐싱을 담당하는 서비스
 * - 상품 검색 API 호출
 * - 상품 정보 추출 및 변환
 * - Redis 캐싱 처리
 * - ProductInfo 객체 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchStreamService {
    
    private final ProductSearchService productSearchService;
    private final ProductCacheService productCacheService;
    private final RedisTemplate<String, String> redisTemplate;
    private final StreamMetricsService streamMetricsService;
    
    /**
     * 상품 검색 및 캐싱 처리
     * @param message 검색할 메시지
     * @return 상품 정보 리스트
     */
    public List<ProductInfo> searchAndCacheProducts(String message) {
        List<ProductInfo> products = new ArrayList<>();
        
        try {
            // 상품 검색 API 호출 시작
            var timerSample = streamMetricsService.startProductSearchApiCall();
            
            // 상품 검색 실행
            Map<String, Object> searchResult = productSearchService.searchProducts(message);
            
            if (searchResult != null) {
                // 상품 검색 성공 메트릭
                streamMetricsService.endProductSearchApiCall(timerSample, true);
                
                // 상품 캐싱 처리
                cacheProductsFromSearchResult(searchResult);
                
                // 상품 정보 추출
                List<String> productImageUrls = productSearchService.extractProductImageUrls(searchResult);
                List<String> productIds = productCacheService.extractProductIds(searchResult);
                
                // Redis에 상품 URL 캐싱
                cacheProductUrlsToRedis(productIds, productImageUrls);
                
                // ProductInfo 객체 생성
                products = createProductInfoList(productIds, productImageUrls);
                
                log.info("상품 검색 및 캐싱 완료: message={}, productCount={}", message, products.size());
                
            } else {
                // 상품 검색 실패 메트릭
                streamMetricsService.endProductSearchApiCall(timerSample, false);
                log.warn("상품 검색 결과가 null입니다: message={}", message);
            }
            
        } catch (Exception e) {
            log.error("상품 검색 및 캐싱 처리 중 오류: message={}, error={}", message, e.getMessage(), e);
            throw new RuntimeException("상품 검색 및 캐싱 처리 실패", e);
        }
        
        return products;
    }
    
    /**
     * 상품 캐싱 처리
     * @param searchResult 검색 결과
     */
    private void cacheProductsFromSearchResult(Map<String, Object> searchResult) {
        try {
            productCacheService.cacheProductsFromSearchResult(searchResult);
            log.debug("상품 캐싱 완료");
        } catch (Exception e) {
            log.warn("상품 캐싱 오류: {}", e.getMessage());
            // 캐싱 실패는 전체 프로세스를 중단시키지 않음
        }
    }
    
    /**
     * 상품 URL을 Redis에 캐싱
     * @param productIds 상품 ID 리스트
     * @param productImageUrls 상품 이미지 URL 리스트
     */
    private void cacheProductUrlsToRedis(List<String> productIds, List<String> productImageUrls) {
        try {
            // product URL을 Redis에 저장 (600분 만료)
            // 인코딩/디코딩 없이 원본 presigned URL 그대로 저장
            for (int i = 0; i < productIds.size() && i < productImageUrls.size(); i++) {
                try {
                    String productId = productIds.get(i);
                    String productUrl = productImageUrls.get(i);
                    
                    if (productId != null && productUrl != null && 
                        !productId.trim().isEmpty() && !productUrl.trim().isEmpty()) {
                        
                        String redisKey = "product_url_" + productId.trim();
                        
                        try {
                            redisTemplate.opsForValue().set(
                                redisKey, 
                                productUrl.trim(), 
                                36000, // 600분 = 36000초
                                TimeUnit.SECONDS
                            );
                            
                            log.info("Product URL saved to Redis (raw): key={}, length(original)={}", 
                                redisKey, productUrl.trim().length());
                                
                        } catch (Exception encodingException) {
                            // 실패 시 원본 URL 직접 저장 시도
                            redisTemplate.opsForValue().set(
                                redisKey, 
                                productUrl.trim(), 
                                36000, 
                                TimeUnit.SECONDS
                            );
                            
                            log.warn("Saving raw URL with fallback: key={}, error={}", 
                                redisKey, encodingException.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to save product URL to Redis from search result: productId={}, productUrl={}, error={}", 
                        productIds.get(i), productImageUrls.get(i), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Redis 상품 URL 캐싱 실패: error={}", e.getMessage(), e);
            // Redis 캐싱 실패는 전체 프로세스를 중단시키지 않음
        }
    }
    
    /**
     * ProductInfo 객체 리스트 생성
     * @param productIds 상품 ID 리스트
     * @param productImageUrls 상품 이미지 URL 리스트
     * @return ProductInfo 객체 리스트
     */
    private List<ProductInfo> createProductInfoList(List<String> productIds, List<String> productImageUrls) {
        List<ProductInfo> products = new ArrayList<>();
        
        try {
            int minSize = Math.min(productImageUrls.size(), productIds.size());
            
            for (int i = 0; i < minSize; i++) {
                ProductInfo productInfo = ProductInfo.builder()
                        .productUrl(productImageUrls.get(i))
                        .productId(productIds.get(i))
                        .build();
                products.add(productInfo);
            }
            
            log.debug("ProductInfo 객체 리스트 생성 완료: count={}", products.size());
            
        } catch (Exception e) {
            log.error("ProductInfo 객체 리스트 생성 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("ProductInfo 객체 리스트 생성 실패", e);
        }
        
        return products;
    }
    
    /**
     * 상품 검색만 실행 (캐싱 없음)
     * @param message 검색할 메시지
     * @return 검색 결과 Map
     */
    public Map<String, Object> searchProductsOnly(String message) {
        try {
            var timerSample = streamMetricsService.startProductSearchApiCall();
            
            Map<String, Object> searchResult = productSearchService.searchProducts(message);
            
            if (searchResult != null) {
                streamMetricsService.endProductSearchApiCall(timerSample, true);
            } else {
                streamMetricsService.endProductSearchApiCall(timerSample, false);
            }
            
            return searchResult;
            
        } catch (Exception e) {
            log.error("상품 검색 실패: message={}, error={}", message, e.getMessage(), e);
            throw new RuntimeException("상품 검색 실패", e);
        }
    }
    
    /**
     * 상품 ID 리스트 추출
     * @param searchResult 검색 결과
     * @return 상품 ID 리스트
     */
    public List<String> extractProductIds(Map<String, Object> searchResult) {
        try {
            return productCacheService.extractProductIds(searchResult);
        } catch (Exception e) {
            log.error("상품 ID 추출 실패: error={}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 상품 이미지 URL 리스트 추출
     * @param searchResult 검색 결과
     * @return 상품 이미지 URL 리스트
     */
    public List<String> extractProductImageUrls(Map<String, Object> searchResult) {
        try {
            return productSearchService.extractProductImageUrls(searchResult);
        } catch (Exception e) {
            log.error("상품 이미지 URL 추출 실패: error={}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
