package com.thefirsttake.app.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductLikeRequestDto {
    // 좋아요 등록 요청용
    private String productId;         // 상품 ID
    private Long userId;              // 로그인 사용자 ID (nullable)
    private String sessionId;         // 비로그인 사용자 세션 ID (nullable)
    private Boolean isLiked;          // true: 좋아요, false: 취소
}
