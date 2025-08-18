package com.thefirsttake.app.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상품 정보 DTO
 * - 상품 이미지 URL과 상품 ID를 함께 담는 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInfo {
    
    /**
     * 상품 이미지 URL
     */
    @JsonProperty("product_url")
    private String productUrl;
    
    /**
     * 상품 ID
     */
    @JsonProperty("product_id")
    private String productId;
}
